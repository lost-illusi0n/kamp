package net.lostillusion.kamp.transport

import kotlinx.coroutines.flow.SharedFlow
import net.lostillusion.kamp.WampMessage

public interface WampTransportSocket {
    public val incoming: SharedFlow<WampMessage>

    public suspend fun send(message: WampMessage)
}