package net.lostillusion.kamp

import io.ktor.util.network.*
import net.lostillusion.kamp.transport.WampTransportSocketClient
import net.lostillusion.kamp.transport.default
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public class WampClient(
    private val socketClient: WampTransportSocketClient = WampTransportSocketClient.default(),
) {
    public suspend fun connect(
        url: NetworkAddress,
        config: SessionConfig,
        sessionCoroutineContext: CoroutineContext = EmptyCoroutineContext
    ): DefaultWampSession {
        val socket = socketClient.connect(url, sessionCoroutineContext)
        val session = DefaultWampSession(socket, config, sessionCoroutineContext)
        session.hello()
        return session
    }

    public suspend fun connect(
        url: NetworkAddress,
        realm: String,
        config: SessionConfigBuilder.() -> Unit = { }
    ): DefaultWampSession = connect(url, SessionConfigBuilder(realm).apply(config).build())
}