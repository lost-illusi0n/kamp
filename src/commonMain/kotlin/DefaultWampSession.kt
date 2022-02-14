package net.lostillusion.kamp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import net.lostillusion.kamp.transport.WampTransportSocket
import net.lostillusion.kamp.transport.firstOf
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

public class DefaultWampSession(
    public val socket: WampTransportSocket,
    public val config: SessionConfig,
    coroutineContext: CoroutineContext
) : WampSession {
    public val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext + CoroutineName("wamp-session"))

    private var session: Id by Delegates.notNull()

    public override val incoming: SharedFlow<WampMessage> = socket.incoming

    internal suspend fun hello() {
        socket.send(config.intoHello())
        val welcome = socket.incoming.firstOf<WampMessage.Welcome>()
        session = welcome.session
    }

    override suspend fun goodbye(message: String) {
        socket.send(
            WampMessage.Goodbye(
                details = WampMessage.Goodbye.Details(message),
                reason = WampClose.SystemShutdown
            )
        )

        val remoteGoodbye = incoming.firstOf<WampMessage.Goodbye>()

        socket.close()

        println("socket closed")

        scope.cancel()

        require(remoteGoodbye.reason == WampClose.GoodbyeAndOut) { "didn't get a goodbye and out" }
    }


    // rpc, pubsub, convenience methods
}