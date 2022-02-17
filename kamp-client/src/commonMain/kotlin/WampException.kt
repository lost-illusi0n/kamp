package net.lostillusion.kamp.client

import io.ktor.util.network.*

public open class WampException(message: String, cause: Throwable? = null): RuntimeException(message, cause)

/**
 * Timed out while connecting to WAMP transport / protocol.
 */
public class WampTimeout(url: NetworkAddress, message: String): WampException(message)

/**
 * Exception on the transport connection level.
 */
public open class WampTransportException(url: NetworkAddress, message: String, cause: Throwable? = null) : WampException(message, cause)

/**
 * Exception on the WAMP protocol level.
 */
public open class WampSessionException(url: NetworkAddress, message: String, cause: Throwable? = null) : WampException(message, cause)
