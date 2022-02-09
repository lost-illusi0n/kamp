package net.lostillusion.kamp.format

import io.ktor.utils.io.core.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class BinaryEncoder : AbstractEncoder() {
    public val output = mutableListOf<Byte>()
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun encodeByte(value: Byte) {
        println("encoding byte")
        output.add(value)
    }

    override fun encodeString(value: String) {
        println("encoding string: $value")
        output.addAll(value.toByteArray().toTypedArray())
    }

    public fun encodeByteArray(value: ByteArray) {
        println("encoding byte array")
        output.addAll(value.toTypedArray())
    }
}