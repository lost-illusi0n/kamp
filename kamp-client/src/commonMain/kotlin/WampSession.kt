package net.lostillusion.kamp.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow

public interface WampSession {
    public val scope: CoroutineScope

    public val incoming: SharedFlow<WampMessage>

    public suspend fun send(message: WampMessage)

    public suspend fun goodbye(message: String)

    public suspend fun call(
        procedure: URI,
        args: Arguments? = null,
        argsKw: ArgumentsKw? = null,
    ): WampCallResponse

    public suspend fun register(
        procedure: URI,
    ): WampRegisterResponse

    public suspend fun unregister(
        registration: Id
    ): WampUnregisterResponse

    public suspend fun yield(
        requestId: Id,
        arguments: Arguments = emptyList(),
        argumentsKw: ArgumentsKw = emptyMap()
    )
}