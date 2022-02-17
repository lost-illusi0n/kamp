package net.lostillusion.kamp.client

import io.ktor.util.network.*
import net.lostillusion.kamp.client.transport.WampTransportSocketClient
import net.lostillusion.kamp.client.transport.default
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public class WampClient(
    private val socketClient: WampTransportSocketClient = WampTransportSocketClient.default(),
) {
    @Throws(WampException::class)
    public suspend fun connect(
        url: NetworkAddress,
        config: SessionConfig,
        coroutineContext: CoroutineContext = EmptyCoroutineContext
    ): DefaultWampSession {
        val socket = socketClient.connect(url, coroutineContext)
        val session = DefaultWampSession(socket, config, coroutineContext)
        session.hello()
        return session
    }

    @Throws(WampException::class)
    public suspend fun connect(
        host: NetworkAddress,
        realm: String,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        config: SessionConfigBuilder.() -> Unit = { }
    ): DefaultWampSession = connect(host, SessionConfigBuilder(realm).apply(config).build(), coroutineContext)
}