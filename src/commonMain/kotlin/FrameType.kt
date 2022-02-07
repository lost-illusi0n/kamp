package net.lostillusion.kamp

public sealed class FrameType(public val raw: Byte) {
    public object Regular : FrameType(0)
    public object Ping : FrameType(1)
    public object Pong : FrameType(2)

    public class Unknown(raw: Byte) : FrameType(raw)

    public companion object {
        public fun from(raw: Byte): FrameType = when (raw.toInt()) {
            0 -> Regular
            1 -> Ping
            2 -> Pong
            else -> Unknown(raw)
        }
    }
}
