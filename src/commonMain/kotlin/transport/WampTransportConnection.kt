package net.lostillusion.kamp.transport

import io.ktor.util.network.*

public interface WampTransportConnection {
    public val remoteAddress: NetworkAddress
}