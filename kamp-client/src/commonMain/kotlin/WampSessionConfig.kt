package net.lostillusion.kamp.client

import kotlinx.serialization.json.JsonObject

public data class WampSessionConfig(val agent: String?, val realm: String, val roles: Set<WampRole>, val messageTimeout: Long)

public class WampSessionConfigBuilder(public val realm: String) {
    private companion object {
        private val DEFAULT_ROLES = setOf(WampRole.Caller, WampRole.Callee, WampRole.Publisher, WampRole.Subscriber)
    }

    public val roles: MutableSet<WampRole> = mutableSetOf()
    public var agent: String? = null

    /**
     * The amount of time the session should wait when expecting a message response back.
     */
    public var messageTimeout: Long = 5000

    public fun callee() {
        roles += WampRole.Callee
    }

    public fun caller() {
        roles += WampRole.Caller
    }

    public fun publisher() {
        roles += WampRole.Publisher
    }

    public fun subscriber() {
        roles += WampRole.Subscriber
    }

    public fun build(): WampSessionConfig =
        WampSessionConfig(
            agent,
            realm,
            roles.ifEmpty { DEFAULT_ROLES },
            messageTimeout
        )
}

internal fun WampSessionConfig.intoHello(): WampMessage.Hello = WampMessage.Hello(
    realm = realm,
    details = WampMessage.Hello.Details(
        agent = agent,
        roles = roles.associateWith { JsonObject(emptyMap()) }
    )
)