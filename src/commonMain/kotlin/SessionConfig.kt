package net.lostillusion.kamp

import kotlinx.serialization.json.JsonObject

public data class SessionConfig(val agent: String?, val realm: String, val roles: Set<Role>)

public class SessionConfigBuilder(public val realm: String) {
    public val roles: MutableSet<Role> = mutableSetOf()
    public var agent: String? = null

    public fun callee() {
        roles += Role.Callee
    }

    public fun caller() {
        roles += Role.Caller
    }

    public fun build(): SessionConfig = SessionConfig(agent, realm, roles)
}

internal fun SessionConfig.intoHello(): HelloWampMessage = HelloWampMessage(
    realm = realm,
    details = HelloWampMessage.Details(
        agent = agent,
        roles = roles.associateWith { JsonObject(emptyMap()) }
    )
)