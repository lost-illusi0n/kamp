package net.lostillusion.kamp

import kotlinx.coroutines.flow.SharedFlow

public interface Session {
    public val incoming: SharedFlow<Message>

    public suspend fun connect(config: SessionConfig)

    public suspend fun goodbye(message: String) {
        send(GoodbyeMessage(GoodbyeMessage.Details(message), "TODO"))
    }

    public suspend fun send(message: Message)
}