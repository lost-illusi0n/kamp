package net.lostillusion.kamp.transport

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import net.lostillusion.kamp.WampSerializerType
import net.lostillusion.kamp.actualSerializer
import kotlin.experimental.and

public const val WAMP_MAGIC: Byte = 0x7F

@Serializable
public sealed class WampTransportPacket(public val identifier: Byte) {
    public object Serializer : KSerializer<WampTransportPacket> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Packet") {
            element("identifier", Byte.serializer().descriptor)
            element("rest", ByteArraySerializer().descriptor)
        }

        override fun deserialize(decoder: Decoder): WampTransportPacket {
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

                    // wamp message
                    TODO()
                }
            }
        }

        @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: WampTransportPacket) {
            println("SERIALIZING")

            encoder.encodeByte(value.identifier)

            when (value) {
                is Handshake -> Handshake.serializer().serialize(encoder, value)
                else -> TODO()
            }
        }
    }

    @Serializable(with = Handshake.Serializer::class)
    public data class Handshake(public val length: Byte, public val serializer: WampSerializerType): WampTransportPacket(WAMP_MAGIC) {
        public object Serializer : KSerializer<Handshake> {
            override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

            override fun deserialize(decoder: Decoder): Handshake {
                val data = decoder.decodeByte()

                // LLLL SSSS
                val length = (data.toInt() shr 4).toByte()
                val serializer = WampSerializerType.from(data and 0x0F)

                // padding
                repeat(2) { decoder.decodeByte() }

                return Handshake(length, serializer)
            }

            override fun serialize(encoder: Encoder, value: Handshake) {
                println("hadnshake serialzier")

                val data = with(value) { (length.toInt() shl 4) or serializer.raw.toInt() }

                encoder.encodeByte(data.toByte())

                // padding
                repeat(2) { encoder.encodeByte(0) }
            }
        }
    }

    @Serializable
    public class Error(): WampTransportPacket(WAMP_MAGIC)
    public class Frame(): WampTransportPacket(0)
}
