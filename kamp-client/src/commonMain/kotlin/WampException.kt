package net.lostillusion.kamp.client

import io.ktor.util.network.*

public open class WampException(public val url: NetworkAddress, message: String, cause: Throwable? = null): RuntimeException(message, cause)

/**
 * Timed out while connecting to WAMP transport / protocol.
 */
public class WampTimeout(url: NetworkAddress, message: String): WampException(url, message)

/**
 * Failed to connect on the transport connection level.
 */
public open class WampTransportException(url: NetworkAddress, message: String, cause: Throwable? = null) : WampException(url, message, cause)

/**
 * Failed to connect on the WAMP protocol level.
 */
public open class WampSessionException(url: NetworkAddress, message: String, cause: Throwable? = null) : WampException(url, message, cause)
