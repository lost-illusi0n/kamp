package net.lostillusion.kamp.transport.raw

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.lostillusion.kamp.transport.raw.WampRawTransportPacket.Frame.Ping
import net.lostillusion.kamp.transport.raw.WampRawTransportPacket.Frame.Pong

internal class PingPong(connection: WampRawTransportConnection) {
    init {
        connection.incoming
            .filterIsInstance<Ping>()
            .onEach { connection.send(Pong(it.payload)) }
            .launchIn(connection.scope)
    }
}