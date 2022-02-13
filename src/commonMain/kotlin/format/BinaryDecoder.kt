package format

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import net.lostillusion.kamp.format.BinarySize

@OptIn(ExperimentalSerializationApi::class)
internal class BinaryDecoder(val input: ByteArray, var elementsCount: Int = 0, var cursor: Int = 0) :
    AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun decodeByte(): Byte {
        val byte = input[cursor]
        cursor += 1
        return byte
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        BinaryDecoder(input, descriptor.elementsCount, cursor)

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = throw NotImplementedError()

    fun <Size : BinarySize<Size>> decodeCollectionSize(
        sizeSerializer: KSerializer<Size>
    ): Size = decodeSerializableValue(sizeSerializer).also { elementsCount = it.asInt }

    override fun decodeSequentially(): Boolean = true

//    @OptIn(InternalSerializationApi::class)
//    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
//        for (annotation in descriptor.getElementAnnotations(elementIndex)) {
//            if (annotation is BinaryLength) {
//                val lengthDescriptor = descriptor.getElementDescriptor(descriptor.getElementIndex(TODO()))
//
//                val length = decodeSerializableElement(lengthDescriptor, elementIndex, lengthDescriptor.capturedKClass!!.serializer())
//
//                return (length as Number).toInt()
//            }
//        }
//
//        error("collection without binary length: $descriptor")
//    }
}
