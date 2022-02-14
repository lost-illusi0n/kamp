package net.lostillusion.kamp

import kotlinx.serialization.json.JsonObject

public data class SessionConfig(val agent: String?, val realm: String, val roles: Set<Role>)

public class SessionConfigBuilder(public val realm: String) {
    private companion object {
        private val DEFAULT_ROLES = setOf(Role.Caller, Role.Callee, Role.Publisher, Role.Subscriber)
    }

    public val roles: MutableSet<Role> = mutableSetOf()
    public var agent: String? = null

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
            roles.ifEmpty { DEFAULT_ROLES }
        )
}

internal fun SessionConfig.intoHello(): WampMessage.Hello = WampMessage.Hello(
    realm = realm,
    details = WampMessage.Hello.Details(
        agent = agent,
        roles = roles.associateWith { JsonObject(emptyMap()) }
    )
)