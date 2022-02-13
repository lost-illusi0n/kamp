package net.lostillusion.kamp.transport.raw

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.lostillusion.kamp.transport.WampTransportConnection

public class WampRawTransportConnection : WampTransportConnection {
    private val _incoming: MutableSharedFlow<WampRawTransportPacket> = MutableSharedFlow()
    public val incoming: SharedFlow<WampRawTransportPacket> = _incoming

    public suspend fun send(packet: WampRawTransportPacket) {

    }
}