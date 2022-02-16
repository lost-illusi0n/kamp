package net.lostillusion.kamp.transport.raw

import kotlinx.coroutines.flow.*
import net.lostillusion.kamp.WampMessage
import net.lostillusion.kamp.transport.WampTransportSocket

public class WampRawTransportSocket(
    public val connection: WampRawTransportConnection
): WampTransportSocket {
    override val incoming: Flow<WampMessage> = connection
        .incomingPackets
        .catch { close(it); throw it }
        .filterIsInstance<WampRawTransportPacket.Frame.Message>()
        .map { it.message }

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