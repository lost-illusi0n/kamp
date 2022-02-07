package net.lostillusion.kamp

public sealed class WampClose(public val reason: String) {
    public object SystemShutdown : WampClose("wamp.close.system_shutdown")
    public object CloseRealm : WampClose("wamp.close.close_realm")
    public object GoodbyeAndOut : WampClose("wamp.close.goodbye_and_out")
    public object ProtocolViolation : WampClose("wamp.close.protocol_violation")
}
