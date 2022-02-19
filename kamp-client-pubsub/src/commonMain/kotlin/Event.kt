@file:OptIn(InternalSerializationApi::class)

package net.lostillusion.kamp.client.pubsub

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import net.lostillusion.kamp.client.Arguments
import net.lostillusion.kamp.client.ArgumentsKw
import net.lostillusion.kamp.client.URI

public sealed class Event(public val topic: URI) {
    public class Nothing(topic: URI) : Event(topic)

    public class Single<E>(topic: URI, public val eventSerializer: KSerializer<E>) : Event(topic) {
        internal fun convertToEvent(args: Arguments) = convertFromArgs(args, eventSerializer)
        internal fun convertFromEvent(event: E) = convertToArgs(event, eventSerializer)
    }

    public class Full<E, EKw>(
        topic: URI,
        public val eventSerializer: KSerializer<E>,
        public val eventKwSerializer: KSerializer<EKw>,
    ) : Event(topic) {
        internal fun convertToEvent(args: Arguments, argsKw: ArgumentsKw): Pair<E, EKw> {
            val event = convertFromArgs(args, eventSerializer)
            val eventKw = convertFromArgsKw(argsKw, eventKwSerializer)

            return event to eventKw
        }
        internal fun convertFromEvent(event: E, eventKw: EKw): Pair<Arguments, ArgumentsKw> {
            val args = convertToArgs(event, eventSerializer)
            val argsKw = convertToArgsKw(eventKw, eventKwSerializer)

            return args to argsKw
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun <T> convertFromArgs(args: Arguments, serializer: KSerializer<T>): T {
        return when (serializer.descriptor.kind) {
            is PrimitiveKind -> {
                require(args.size == 1)

                Json.decodeFromJsonElement(serializer, args.first())
            }
            StructureKind.LIST -> {
                Json.decodeFromJsonElement(serializer, JsonArray(args))
            }
            is StructureKind -> {
                // names were stripped, get them back
                val obj = JsonObject(
                    args.mapIndexed { index, jsonElement ->
                        serializer.descriptor.getElementName(index) to jsonElement
                    }.associate { it }
                )

                Json.decodeFromJsonElement(serializer, obj)
            }
            else -> error("not supported")
        }
    }

    internal fun <T> convertToArgs(value: T, serializer: KSerializer<T>): Arguments {
        return when (val base = Json.encodeToJsonElement(serializer, value)) {
            is JsonPrimitive -> listOf(base)
            is JsonObject -> base.values.toList()
            is JsonArray -> base
            JsonNull -> emptyList()
        }
    }

    internal fun <T> convertFromArgsKw(argsKw: ArgumentsKw, serializer: KSerializer<T>): T {
        return Json.decodeFromJsonElement(serializer, JsonObject(argsKw))
    }

    internal fun <T> convertToArgsKw(value: T, serializer: KSerializer<T>): ArgumentsKw {
        val base = Json.encodeToJsonElement(serializer, value)

        require(base is JsonObject) { "value must serialize as a JsonObject!" }

        return base
    }
}

public fun event(topic: URI): Event.Nothing = Event.Nothing(topic)

public inline fun <reified E : Any> singleEvent(
    topic: URI,
    eventSerializer: KSerializer<E> = E::class.serializer()
): Event.Single<E> = Event.Single(topic, eventSerializer)

public inline fun <reified E : Any, reified EKw : Any> fullEvent(
    topic: URI,
    eventSerializer: KSerializer<E> = E::class.serializer(),
    eventKwSerializer: KSerializer<EKw> = EKw::class.serializer()
): Event.Full<E, EKw> = Event.Full(topic, eventSerializer, eventKwSerializer)