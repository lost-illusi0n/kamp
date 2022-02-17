package net.lostillusion.kamp.client

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializerOrNull
import net.lostillusion.kamp.client.format.Binary.serializersModule
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal fun <T : Any> T.actualSerializer(klass: KClass<out T>): KSerializer<T> {
    return (serializersModule.getPolymorphic(klass as KClass<T>, this) ?: klass.serializerOrNull()) as KSerializer<T>
}