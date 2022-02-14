package net.lostillusion.kamp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializerOrNull
import net.lostillusion.kamp.format.Binary.serializersModule
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal inline fun <T : Any> T.actualSerializer(encoder: Encoder, klass: KClass<out T>): KSerializer<T> {
    return (serializersModule.getPolymorphic(klass as KClass<T>, this) ?: klass.serializerOrNull()) as KSerializer<T>
}