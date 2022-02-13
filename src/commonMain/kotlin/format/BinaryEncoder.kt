package net.lostillusion.kamp.format

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class BinaryEncoder : AbstractEncoder() {
    public val output = mutableListOf<Byte>()
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun encodeByte(value: Byte) {
        output.add(value)
    }

    fun <Size> beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Size,
        sizeSerializer: KSerializer<Size>
    ): CompositeEncoder {
        encodeSerializableValue(sizeSerializer, collectionSize)
        return this
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int
    ): CompositeEncoder = throw NotImplementedError()
}

@OptIn(InternalSerializationApi::class)
internal inline fun <Size : Any> BinaryEncoder.encodeCollection(
    descriptor: SerialDescriptor,
    size: Size,
    sizeSerializer: KSerializer<Size>,
    crossinline block: CompositeEncoder.() -> Unit
) {
    with(beginCollection(descriptor, size, sizeSerializer)) {
        block()
        endStructure(descriptor)
    }
}