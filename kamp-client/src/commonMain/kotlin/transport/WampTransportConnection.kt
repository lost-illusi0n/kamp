package net.lostillusion.kamp.client.transport

import io.ktor.util.network.*

public interface WampTransportConnection {
    public val remoteAddress: NetworkAddress
}