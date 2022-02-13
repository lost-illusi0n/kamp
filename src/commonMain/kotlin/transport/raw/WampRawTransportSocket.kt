package net.lostillusion.kamp.transport.raw

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import net.lostillusion.kamp.WampMessage
import net.lostillusion.kamp.transport.WampTransportSocket

public class WampRawTransportSocket(
    public val connection: WampRawTransportConnection
): WampTransportSocket {
    override val incoming: SharedFlow<WampMessage> = connection
        .incoming
        .filterIsInstance<WampRawTransportPacket.Frame.Message>()
        .map { it.message }
        .shareIn(GlobalScope, SharingStarted.Eagerly)

    override suspend fun send(message: WampMessage) {
    }
}