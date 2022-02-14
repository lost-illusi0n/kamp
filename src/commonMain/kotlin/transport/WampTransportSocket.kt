package net.lostillusion.kamp.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import net.lostillusion.kamp.WampMessage

public interface WampTransportSocket {
    public val incoming: SharedFlow<WampMessage>

    public suspend fun send(message: WampMessage)

    public suspend fun close()
}

public suspend inline fun <reified Type : WampMessage> Flow<WampMessage>.firstOf(): Type =
    filterIsInstance<Type>().first()