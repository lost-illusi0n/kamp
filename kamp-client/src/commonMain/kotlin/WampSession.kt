package net.lostillusion.kamp.client

import kotlinx.coroutines.flow.SharedFlow

public interface WampSession {
    public val incoming: SharedFlow<WampMessage>

    public suspend fun send(message: WampMessage)

    public suspend fun goodbye(message: String)

    public suspend fun call(
        procedure: URI,
        args: Arguments = emptyList(),
        kwArgs: ArgumentsKw = emptyMap(),
    ): WampCallResponse
}

// i'm not sure if i like this
public sealed interface CallResponse {
    public class Success(public val result: WampMessage.Result): CallResponse
    public class Error(public val error: WampMessage.Error): CallResponse
}