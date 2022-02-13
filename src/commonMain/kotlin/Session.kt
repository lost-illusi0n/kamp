package net.lostillusion.kamp

import kotlinx.coroutines.flow.SharedFlow

public interface Session {
    public val incoming: SharedFlow<WampMessage>

    public suspend fun connect(config: SessionConfig)

    public suspend fun goodbye(message: String) {
        send(GoodbyeWampMessage(GoodbyeWampMessage.Details(message), "TODO"))
    }

    public suspend fun send(message: WampMessage)
}