package net.lostillusion.kamp.client.transport.raw

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WampRawTransportError.Serializer::class)
public enum class WampRawTransportError(public val raw: Byte) {
    Illegal(0),
    UnsupportedSerializer(1),
    UnacceptableMaximumMessageLength(2),
    UseOfReservedByteError(3),
    MaximumConnectionCountError(4);

    public object Serializer : KSerializer<WampRawTransportError> {
        override val descriptor: SerialDescriptor = Byte.serializer().descriptor

        override fun deserialize(decoder: Decoder): WampRawTransportError {
            val raw = (decoder.decodeByte().toInt() shr 4).toByte()

            return values().find { it.raw == raw } ?: error("unknown transport error: $raw")
        }

        override fun serialize(encoder: Encoder, value: WampRawTransportError) {
            encoder.encodeByte((value.raw.toInt() shl 4).toByte())
        }
    }
}