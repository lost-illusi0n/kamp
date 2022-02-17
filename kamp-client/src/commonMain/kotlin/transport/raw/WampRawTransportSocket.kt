package net.lostillusion.kamp.client.transport.raw

import io.ktor.util.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import net.lostillusion.kamp.client.WampMessage
import net.lostillusion.kamp.client.transport.WampTransportSocket

public class WampRawTransportSocket(
    public val connection: WampRawTransportConnection
): WampTransportSocket {
    override val incoming: Flow<WampMessage> = connection
        .incomingPackets
        .catch { close(it); throw it }
        .filterIsInstance<WampRawTransportPacket.Frame.Message>()
        .map { it.message }

    override val host: NetworkAddress = connection.remoteAddress

    private suspend fun send(packet: WampRawTransportPacket) {
        connection.send(packet)
    }

    override suspend fun send(message: WampMessage) {
        send(WampRawTransportPacket.Frame.Message(message))
    }

    override suspend fun close(cause: Throwable?) {
        if (cause !is WampTransportClosedException) {
            connection.close()
        }
    }
}