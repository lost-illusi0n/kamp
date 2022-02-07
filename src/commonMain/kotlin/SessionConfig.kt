package net.lostillusion.kamp

public data class SessionConfig(val agent: String, val realm: String, val roles: Set<Role>)

public class SessionConfigBuilder(public val realm: String) {
    public val roles: MutableSet<Role> = mutableSetOf()
    public var agent: String = "kamp"

    public fun callee() {
        roles += Role.Callee
    }

    public fun caller() {
        roles += Role.Caller
    }

    public fun build(): SessionConfig = SessionConfig(agent, realm, roles)
}

public fun SessionConfig(realm: String, builder: SessionConfigBuilder.() -> Unit): SessionConfig =
    SessionConfigBuilder(realm).also(builder).build()