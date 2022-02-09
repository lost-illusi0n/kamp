package net.lostillusion.kamp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializerOrNull

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal inline fun <reified T : Any> Encoder.actualSerializer(value: T): KSerializer<T> {
    return (serializersModule.getPolymorphic(T::class, value) ?: T::class.serializerOrNull()) as KSerializer<T>
}