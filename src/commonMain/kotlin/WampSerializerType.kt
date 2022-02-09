package net.lostillusion.kamp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WampSerializerType.Serializer::class)
public sealed class WampSerializerType(public val raw: Byte) {
    public object Json : WampSerializerType(1)
    public object MessagePack : WampSerializerType(2)
    public class Unknown(raw: Byte) : WampSerializerType(raw)

    public companion object {
        public fun from(raw: Byte): WampSerializerType = when (raw.toInt()) {
            1 -> Json
            2 -> MessagePack
            else -> Unknown(raw)
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