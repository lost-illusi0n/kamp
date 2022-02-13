package net.lostillusion.kamp.format

import io.ktor.utils.io.core.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal open class BinaryStringSerializer<Size: BinarySize<Size>>(private val binarySerializer: BinaryByteArraySerializer<Size>) : BinaryLengthBasedSerializer<Size, String> {
    override val descriptor: SerialDescriptor = binarySerializer.descriptor

    override val String.collectionSize: Size
        get() = throw NotImplementedError()

    override val sizeSerializer: KSerializer<Size>
        get() = binarySerializer.sizeSerializer

    override fun deserialize(decoder: Decoder): String {
        return binarySerializer.deserialize(decoder).decodeToString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        binarySerializer.serialize(encoder, value.toByteArray())
    }
}