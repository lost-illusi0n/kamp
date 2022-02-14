package net.lostillusion.kamp

import kotlinx.coroutines.flow.SharedFlow

public interface WampSession {
    public val incoming: SharedFlow<WampMessage>

    public suspend fun goodbye(message: String)
}