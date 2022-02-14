package net.lostillusion.kamp.transport.raw

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.lostillusion.kamp.WampMessage
import net.lostillusion.kamp.transport.WampTransportSocket
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public class WampRawTransportSocket(
    public val connection: WampRawTransportConnection,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
): WampTransportSocket {
    public val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext + CoroutineName("wamp-raw-transport-socket"))

    override val incoming: SharedFlow<WampMessage> = connection
        .incoming
        .filterIsInstance<WampRawTransportPacket.Frame.Message>()
        .map { it.message }
        .shareIn(scope, SharingStarted.Eagerly)

    private suspend fun send(packet: WampRawTransportPacket) {
        connection.send(packet)
    }

    override suspend fun send(message: WampMessage) {
        send(WampRawTransportPacket.Frame.Message(message))
    }

    override suspend fun close() {
        connection.close()
        scope.cancel()
    }
}