package net.lostillusion.kamp.transport.raw

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.lostillusion.kamp.BinaryWampMessageLengthBasedByteArraySerializer
import net.lostillusion.kamp.BinaryWampMessageLengthBasedStringSerializer
import net.lostillusion.kamp.WampMessage
import net.lostillusion.kamp.WampSerializerType
import net.lostillusion.kamp.format.BinaryJsonString
import net.lostillusion.kamp.format.BinaryLength
import net.lostillusion.kamp.format.BinaryPadding
import net.lostillusion.kamp.format.DefaultBinarySerializer
import kotlin.experimental.and

public const val WAMP_MAGIC: Byte = 0x7F

@Serializable
public sealed class WampRawTransportPacket(public val identifier: Byte) {
    public object Serializer : KSerializer<WampRawTransportPacket> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Packet") {
            element("identifier", Byte.serializer().descriptor)
        }

        override fun deserialize(decoder: Decoder): WampRawTransportPacket {
            when (val identifier = decoder.decodeByte()) {
                WAMP_MAGIC -> {
                    val lower = identifier and 0x0F
                    // handshake
                    return when (lower.toInt()) {
                        0 -> Error.serializer().deserialize(decoder)
                        else -> Handshake.serializer().deserialize(decoder)
                    }
                }
                else -> {
                    // if this is a transport message then the first 5 bits are reserved and should be 0
                    require(identifier.toInt() shr 3 == 0)

                    // we can't delegate to a separate frame serializer since we would
                    // lose the identifier :(
                    return when (identifier.toInt()) {
                        0 -> Frame.Message.BinarySerializer.deserialize(decoder)
                        1 -> Frame.Ping.BinarySerializer.deserialize(decoder)
                        2 -> Frame.Pong.BinarySerializer.deserialize(decoder)
                        else -> error("missing frame serializer case")
                    }
                }
            }
        }

        override fun serialize(encoder: Encoder, value: WampRawTransportPacket) {
            encoder.encodeByte(value.identifier)

            when (value) {
                is Handshake -> Handshake.serializer().serialize(encoder, value)
                is Error -> Error.serializer().serialize(encoder, value)
                is Frame.Message -> Frame.Message.BinarySerializer.serialize(encoder, value)
                is Frame.Ping -> Frame.Ping.BinarySerializer.serialize(encoder, value)
                is Frame.Pong -> Frame.Pong.BinarySerializer.serialize(encoder, value)
            }
        }
    }

    @Serializable(with = Handshake.Serializer::class)
    public data class Handshake(
        public val length: UByte,
        public val serializer: WampSerializerType
    ) : WampRawTransportPacket(WAMP_MAGIC) {
        public object Serializer : KSerializer<Handshake> {
            override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

            override fun deserialize(decoder: Decoder): Handshake {
                val data = decoder.decodeByte().toUByte()

                // LLLL SSSS
                val length = (data.toInt() shr 4).toUByte()
                val serializer = WampSerializerType.from((data and 0x0Fu).toByte())

                // padding
                repeat(2) { decoder.decodeByte() }

                return Handshake(length, serializer)
            }

            override fun serialize(encoder: Encoder, value: Handshake) {
                val data = with(value) { (length.toInt() shl 4) or serializer.raw.toInt() }

                encoder.encodeByte(data.toByte())

                // padding
                repeat(2) { encoder.encodeByte(0) }
            }
        }
    }

    @Serializable
    public class Error(@BinaryPadding(2) public val error: WampRawTransportError) : WampRawTransportPacket(WAMP_MAGIC)

    public sealed class Frame(identifier: Byte) : WampRawTransportPacket(identifier) {
        @Serializable(with = Message.BinarySerializer::class)
        public data class Message(
            @BinaryJsonString
            @BinaryLength(BinaryWampMessageLengthBasedStringSerializer::class)
            public val message: WampMessage
        ) : Frame(0) {
            internal object BinarySerializer : KSerializer<Message> by DefaultBinarySerializer(Message::class)
        }

        @Serializable(with = Ping.BinarySerializer::class)
        public class Ping(
            @BinaryLength(BinaryWampMessageLengthBasedByteArraySerializer::class)
            public val payload: ByteArray
        ) : Frame(1) {
            internal object BinarySerializer : KSerializer<Ping> by DefaultBinarySerializer(Ping::class)
        }

        @Serializable(with = Pong.BinarySerializer::class)
        public class Pong(
            @BinaryLength(BinaryWampMessageLengthBasedByteArraySerializer::class)
            public val payload: ByteArray
        ) : Frame(3) {
            internal object BinarySerializer : KSerializer<Pong> by DefaultBinarySerializer(Pong::class)
        }
    }
}