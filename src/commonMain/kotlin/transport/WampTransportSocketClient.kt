package net.lostillusion.kamp.transport

import io.ktor.util.network.*
import net.lostillusion.kamp.transport.raw.WampRawTransportSocketClient
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public interface WampTransportSocketClient {
    public suspend fun connect(url: NetworkAddress, coroutineContext: CoroutineContext = EmptyCoroutineContext): WampTransportSocket

    public companion object
}

public fun WampTransportSocketClient.Companion.default(): WampRawTransportSocketClient = WampRawTransportSocketClient()