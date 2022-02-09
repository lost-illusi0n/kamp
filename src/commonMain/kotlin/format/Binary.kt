package net.lostillusion.kamp.format

import format.BinaryDecoder
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

public object Binary : BinaryFormat {
    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val encoder = BinaryEncoder()
        encoder.encodeSerializableValue(serializer, value)
        return encoder.output.toByteArray()
    }
    public inline fun <reified T> encodeToByteArray(value: T): ByteArray = encodeToByteArray(serializer(), value)

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val decoder = BinaryDecoder(bytes)
        return decoder.decodeSerializableValue(deserializer)
    }

    public inline fun <reified T> decodeFromByteArray(value: ByteArray): T = decodeFromByteArray(serializer(), value)
}