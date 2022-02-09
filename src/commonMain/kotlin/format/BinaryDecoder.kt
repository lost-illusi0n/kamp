package format

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class BinaryDecoder(val input: ByteArray) : AbstractDecoder() {
    private var cursor = 0
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule

    private fun readBytes(n: Int): ByteArray {
        val data = input.copyOfRange(cursor, cursor + n)
        cursor += n
        return data
    }

    private fun readByte(): Byte {
        val byte = input[cursor]
        cursor += 1
        return byte
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    public fun decodeString(len: Int): String {
        return readBytes(len).decodeToString()
    }

    public fun decodeByteArray(len: Int): ByteArray {
        return readBytes(len)
    }

    override fun decodeByte(): Byte {
        return readByte()
    }
}
