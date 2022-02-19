package net.lostillusion.kamp.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.lostillusion.kamp.client.format.BinaryByteArraySerializer
import net.lostillusion.kamp.client.format.BinarySize
import net.lostillusion.kamp.client.format.BinaryStringSerializer
import kotlin.jvm.JvmInline
import kotlin.random.Random

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

@Serializable
@JvmInline
public value class Id private constructor(public val value: Long) {
    public companion object {
        // max safe integer (2^53 - 1)
        public const val MAX_VALUE: Long = 9007199254740991

        public fun generateGlobal(): Id = Id(Random.nextLong(1, MAX_VALUE))
        public fun generateRouter(): Id = Id(Random.nextInt().toLong())
        public fun from(value: Long): Id {
            require(value in 1..MAX_VALUE)
            return Id(value)
        }
    }
}

@JvmInline
@Serializable
public value class URI private constructor(public val value: String) {
    public companion object {
        // these two may need to change with an advanced profile implementation
        private val LOOSE_PATTERN = Regex("^([^\\s\\.#]+\\.)*([^\\s\\.#]+)\$")
        private val STRICT_PATTERN = Regex("^([0-9a-z_]+\\.)*([0-9a-z_]+)\$")

        public fun loose(value: String): URI? {
            if(value.startsWith("wamp.") || !LOOSE_PATTERN.matches(value)) return null

            return URI(value)
        }

        public fun strict(value: String): URI? {
            if(value.startsWith("wamp.") || !STRICT_PATTERN.matches(value)) return null

            return URI(value)
        }

        /**
         * Creates a [URI] from [value] without any checks.
         * Use this only when using known good values that follow the URI spec.
         */
        public fun unsafe(value: String): URI = URI(value)
    }

    override fun toString(): String {
        return value
    }
}

internal val String.uri: URI
    get() = URI.unsafe(this)

@Serializable(with = WampMessageLength.Serializer::class)
@JvmInline
public value class WampMessageLength private constructor(public val value: Int) : BinarySize<WampMessageLength> {
    public companion object {
        public operator fun invoke(value: Int): WampMessageLength {
            require(value shr 24 == 0)
            return WampMessageLength(value)
        }
    }

    public operator fun component1(): Byte = (value shr 16).toByte()
    public operator fun component2(): Byte = (value shr 8).toByte()
    public operator fun component3(): Byte = value.toByte()

    public object Serializer : KSerializer<WampMessageLength> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WampMessageLength") {
            element("one", Byte.serializer().descriptor)
            element("two", Byte.serializer().descriptor)
            element("three", Byte.serializer().descriptor)
        }

        override fun deserialize(decoder: Decoder): WampMessageLength {
            var lengthValue = 0

            repeat(3) {
                lengthValue = (lengthValue shl 8) or decoder.decodeByte().toInt()
            }

            return WampMessageLength(lengthValue)
        }

        override fun serialize(encoder: Encoder, value: WampMessageLength) {
            val (three, two, one) = value

            with(encoder) {
                encodeByte(three)
                encodeByte(two)
                encodeByte(one)
            }
        }
    }

    override val asInt: Int
        get() = value

    override val serializer: KSerializer<WampMessageLength>
        get() = Serializer
}

internal object BinaryWampMessageLengthBasedByteArraySerializer : BinaryByteArraySerializer<WampMessageLength>() {
    override val ByteArray.collectionSize: WampMessageLength
        get() = WampMessageLength(size)

    override val sizeSerializer: KSerializer<WampMessageLength>
        get() = WampMessageLength.Serializer
}

internal object BinaryWampMessageLengthBasedStringSerializer : BinaryStringSerializer<WampMessageLength>(BinaryWampMessageLengthBasedByteArraySerializer)