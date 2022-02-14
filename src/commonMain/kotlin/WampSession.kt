package net.lostillusion.kamp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import net.lostillusion.kamp.transport.WampTransportSocket
import net.lostillusion.kamp.transport.firstOf
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

public class WampSession(
    public val socket: WampTransportSocket,
    public val config: SessionConfig,
    coroutineContext: CoroutineContext
) {
    public val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext + CoroutineName("wamp-session"))

    private var session: Id by Delegates.notNull()

    public val incoming: SharedFlow<WampMessage> = socket.incoming

    internal suspend fun hello() {
        socket.send(config.intoHello())
        val welcome = socket.incoming.firstOf<WelcomeWampMessage>()
        session = welcome.session
    }

    // rpc, pubsub, convenience methods
}