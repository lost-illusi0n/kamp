package net.lostillusion.kamp.client

import kotlinx.serialization.json.JsonObject

public data class SessionConfig(val agent: String?, val realm: String, val roles: Set<Role>, val messageTimeout: Long)

public class SessionConfigBuilder(public val realm: String) {
    private companion object {
        private val DEFAULT_ROLES = setOf(Role.Caller, Role.Callee, Role.Publisher, Role.Subscriber)
    }

    public val roles: MutableSet<Role> = mutableSetOf()
    public var agent: String? = null

    /**
     * The amount of time the session should wait when expecting a message response back.
     */
    public var messageTimeout: Long = 5000

    public fun callee() {
        roles += Role.Callee
    }

    public fun caller() {
        roles += Role.Caller
    }

    public fun publisher() {
        roles += Role.Publisher
    }

    public fun subscriber() {
        roles += Role.Subscriber
    }

    public fun build(): SessionConfig =
        SessionConfig(
            agent,
            realm,
            roles.ifEmpty { DEFAULT_ROLES },
            messageTimeout
        )
}

internal fun SessionConfig.intoHello(): WampMessage.Hello = WampMessage.Hello(
    realm = realm,
    details = WampMessage.Hello.Details(
        agent = agent,
        roles = roles.associateWith { JsonObject(emptyMap()) }
    )
)