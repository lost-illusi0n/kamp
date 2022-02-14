package net.lostillusion.kamp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WampClose.Serializer::class)
public sealed class WampClose(public val reason: URI) {
    public object SystemShutdown : WampClose("wamp.close.system_shutdown".uri)
    public object CloseRealm : WampClose("wamp.close.close_realm".uri)
    public object GoodbyeAndOut : WampClose("wamp.close.goodbye_and_out".uri)
    public object ProtocolViolation : WampClose("wamp.close.protocol_violation".uri)
    public class Other(reason: URI) : WampClose(reason)

    public companion object {
        public val values: Array<WampClose> by lazy {
            arrayOf(
                SystemShutdown,
                CloseRealm,
                GoodbyeAndOut,
                ProtocolViolation
            )
        }
    }

    public object Serializer : KSerializer<WampClose> {
        override val descriptor: SerialDescriptor = URI.serializer().descriptor

        override fun deserialize(decoder: Decoder): WampClose {
            val reason = decoder.decodeSerializableValue(URI.serializer())
            return values.find { it.reason == reason } ?: Other(reason)
        }

        override fun serialize(encoder: Encoder, value: WampClose) {
            encoder.encodeSerializableValue(URI.serializer(), value.reason)
        }
    }
}
