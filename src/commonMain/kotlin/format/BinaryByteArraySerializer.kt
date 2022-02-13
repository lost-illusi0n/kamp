package net.lostillusion.kamp.format

import format.BinaryDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.lostillusion.kamp.BinaryWampMessageLengthBasedByteArraySerializer.sizeSerializer

internal abstract class BinaryByteArraySerializer<Size: BinarySize<Size>> : BinaryLengthBasedSerializer<Size, ByteArray> {
    override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder as BinaryEncoder

        encoder.encodeCollection(descriptor, value.collectionSize, sizeSerializer) {
            value.forEachIndexed { i, v -> encodeByteElement(descriptor, i, v) }
        }
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val builder = mutableListOf<Byte>()
        val compDecoder = decoder.beginStructure(descriptor) as BinaryDecoder
        compDecoder.decodeCollectionSize(sizeSerializer)

        while (true) {
            val index = compDecoder.decodeElementIndex(descriptor)
            if (index == CompositeDecoder.DECODE_DONE) break
            builder.add(compDecoder.decodeByteElement(descriptor, index))
        }

        compDecoder.endStructure(descriptor)

        decoder as BinaryDecoder
        decoder.cursor = compDecoder.cursor

        return builder.toByteArray()
    }
}