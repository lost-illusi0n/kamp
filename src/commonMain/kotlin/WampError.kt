package net.lostillusion.kamp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WampError.Serializer::class)
public sealed class WampError(public val reason: URI) {
    public object InvalidUri : WampError("wamp.error.invalid_uri".uri)
    public object NoSuchProcedure : WampError("wamp.error.no_such_procedure".uri)
    public object NoSuchRegistration : WampError("wamp.error.no_such_registration".uri)
    public object NoSuchSubscription : WampError("wamp.error.no_such_subscription".uri)
    public object InvalidArgument : WampError("wamp.error.invalid_argument".uri)
    public class Unknown(reason: URI) : WampError(reason)

    public companion object {
        /***
         * An array of the predefined Wamp errors.
         */
        public val values: Array<WampError> = arrayOf(
            InvalidUri,
            NoSuchProcedure,
            NoSuchProcedure,
            NoSuchRegistration,
            NoSuchSubscription,
            InvalidArgument
        )

        public fun from(reason: URI): WampError {
            return values.find { it.reason.value.contentEquals(reason.value) } ?: Unknown(reason)
        }
    }

    public object Serializer : KSerializer<WampError> {
        override val descriptor: SerialDescriptor = URI.serializer().descriptor

        override fun deserialize(decoder: Decoder): WampError {
            return from(decoder.decodeSerializableValue(URI.serializer()))
        }

        override fun serialize(encoder: Encoder, value: WampError) {
            encoder.encodeSerializableValue(URI.serializer(), value.reason)
        }
    }
}
