package net.lostillusion.kamp.client

import kotlinx.coroutines.flow.SharedFlow

public interface WampSession {
    public val incoming: SharedFlow<WampMessage>

    public suspend fun send(message: WampMessage)

    public suspend fun goodbye(message: String)

    public suspend fun call(
        procedure: URI,
        args: Arguments? = null,
        argsKw: ArgumentsKw? = null,
    ): WampCallResponse
}