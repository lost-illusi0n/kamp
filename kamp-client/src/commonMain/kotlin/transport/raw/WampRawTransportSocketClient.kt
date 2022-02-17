package net.lostillusion.kamp.client.transport.raw

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.util.network.*
import kotlinx.coroutines.Dispatchers
import net.lostillusion.kamp.client.transport.WampTransportSocket
import net.lostillusion.kamp.client.transport.WampTransportSocketClient
import kotlin.coroutines.CoroutineContext

@OptIn(InternalAPI::class)
public class WampRawTransportSocketClient(
    private val selectorManager: SelectorManager = SelectorManager(Dispatchers.Default),
    private val connectionConfig: WampRawTransportConnectionConfig
) : WampTransportSocketClient {
    public constructor(
        selectorManager: SelectorManager = SelectorManager(Dispatchers.Default),
        connectionConfig: WampRawTransportConnectionConfigBuilder.() -> Unit = { }
    ) : this(
        selectorManager,
        WampRawTransportConnectionConfigBuilder().apply(connectionConfig).build()
    )

    public override suspend fun connect(url: NetworkAddress, coroutineContext: CoroutineContext): WampTransportSocket {
        val socket = aSocket(selectorManager).tcp().connect(url)
        val connection = WampRawTransportConnection(socket, connectionConfig, coroutineContext)
        connection.handshake()
        return WampRawTransportSocket(connection)
    }
}