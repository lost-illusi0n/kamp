package net.lostillusion.kamp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.lostillusion.kamp.format.BinaryByteArraySerializer
import net.lostillusion.kamp.format.BinaryLength
import net.lostillusion.kamp.format.BinarySize
import net.lostillusion.kamp.format.BinaryStringSerializer
import kotlin.jvm.JvmInline
import kotlin.math.pow
import kotlin.random.Random

@Serializable
@JvmInline
public value class Id private constructor(public val value: Long) {
    public companion object {
        public fun generateGlobal(): Id = Id(Random.nextLong(1, 2.toDouble().pow(53).toLong()))
        public fun generateRouter(): Id = Id(Random.nextInt().toLong())
//        public fun generateSession()
    }
}

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