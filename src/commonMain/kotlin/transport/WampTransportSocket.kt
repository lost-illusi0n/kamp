package net.lostillusion.kamp.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import net.lostillusion.kamp.WampMessage

/**
 * An intermediary between a WAMP Session and the underlying transport.
 */
public interface WampTransportSocket {
    public val incoming: Flow<WampMessage>

    public suspend fun send(message: WampMessage)

    public suspend fun close(cause: Throwable? = null)
}

public suspend inline fun <reified Type : WampMessage> Flow<WampMessage>.firstOf(): Type =
    filterIsInstance<Type>().first()