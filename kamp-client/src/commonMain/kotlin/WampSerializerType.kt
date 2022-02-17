package net.lostillusion.kamp.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WampSerializerType.Serializer::class)
public enum class WampSerializerType(public val raw: Byte) {
    Json(1),
    MessagePack(2);

    public companion object {
        public fun from(raw: Byte): WampSerializerType = when (raw.toInt()) {
            1 -> Json
            2 -> MessagePack
            else -> error("unknown serializer type: $raw")
        }
    }

    public object Serializer : KSerializer<WampSerializerType> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SerializerType", PrimitiveKind.BYTE)

        override fun deserialize(decoder: Decoder): WampSerializerType = from(decoder.decodeByte())

        override fun serialize(encoder: Encoder, value: WampSerializerType) {
            encoder.encodeByte(value.raw)
        }
    }
}