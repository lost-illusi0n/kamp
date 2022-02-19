package net.lostillusion.kamp.client

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import net.lostillusion.kamp.client.format.Binary
import kotlin.reflect.KClass

// TODO: open up for extension messages

@OptIn(ExperimentalSerializationApi::class)
@Serializable
public sealed class WampMessage(public val type: WampMessageType) {
    public companion object : KSerializer<WampMessage> {
        override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

        @Suppress("UNCHECKED_CAST")
        @OptIn(InternalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: WampMessage) {
            val output = encoder as JsonEncoder

            val element = output.json.encodeToJsonElement(value.actualSerializer(value::class), value).jsonObject

            output.encodeJsonElement(JsonArray(element.entries.map { it.value }))
        }

        override fun deserialize(decoder: Decoder): WampMessage {
            val input = decoder as JsonDecoder
            val tree = input.decodeJsonElement()

            val type = WampMessageType.from(tree.jsonArray[0].jsonPrimitive.int.toByte())
            val actualSerializer = if (type != WampMessageType.Error) {
                type.messageSerializer
            } else {
                val call = WampMessageType.from(tree.jsonArray[1].jsonPrimitive.int.toByte())

                call.errorSerializer
            }

            return input.json.decodeFromJsonElement(actualSerializer, tree.jsonArray.transform(actualSerializer))
        }

        private fun JsonArray.transform(serializer: KSerializer<*>): JsonObject = JsonObject(
            mapOf(
                *this.mapIndexed { i, e ->
                    serializer.descriptor.getElementName(i) to e
                }.toTypedArray()
            )
        )
    }

    @Serializable
    public data class Hello(val realm: String, val details: Details) : WampMessage(WampMessageType.Hello) {
        @Serializable
        public data class Details(val agent: String? = null, val roles: Map<WampRole, JsonObject>)
    }

    @Serializable
    public data class Welcome(val session: Id, val details: Details) : WampMessage(WampMessageType.Welcome) {
        @Serializable
        public data class Details(val agent: String? = null, val roles: Map<WampRole, JsonObject>)
    }

    @Serializable
// TODO: make a uri type for reason and other similar fields
    public data class Abort(val details: Details, val reason: String) : WampMessage(WampMessageType.Abort) {
        @Serializable
        public data class Details(val message: String)
    }

    @Serializable
    public data class Goodbye(val details: Details = Details(), val reason: WampClose) : WampMessage(WampMessageType.Goodbye) {
        @Serializable
        public data class Details(val message: String? = null)
    }

    @Serializable
    public data class Call(
        val request: Id,
        val options: Options,
        val procedure: URI,
        // these should not be present in serialized form if null
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : WampMessage(WampMessageType.Call) {
        @Serializable
        public class Options
    }

    @Serializable
    public data class Result(
        override val request: Id,
        val details: Details,
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : WampMessage(WampMessageType.Result), WampCallResponse {
        @Serializable
        public class Details
    }

    @Serializable
    public sealed class Error(public val messageType: WampMessageType) : WampMessage(WampMessageType.Error) {
        public object Serializer : KSerializer<Error> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Error") {
                element("messageType", WampMessageType.Serializer.descriptor)
            }

            override fun deserialize(decoder: Decoder): Error {
                return decoder.decodeSerializableValue(WampMessageType.Serializer).errorSerializer.deserialize(decoder)
            }

            override fun serialize(encoder: Encoder, value: Error) {
                encoder.encodeSerializableValue(value.actualSerializer(Error::class), value)
            }
        }
    }

    @Serializable
    public data class CallError(
        override val request: Id,
        val details: Details,
        val error: WampError,
        val args: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : Error(WampMessageType.Call), WampCallResponse {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Register(
        val request: Id,
        val options: Details,
        val procedure: URI
    ) : WampMessage(WampMessageType.Register) {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Registered(
        override val request: Id,
        val registration: Id
    ) : WampMessage(WampMessageType.Registered), WampRegisterResponse

    @Serializable
    public data class RegisterError(
        override val request: Id,
        val details: Details,
        val error: WampError
    ) : Error(WampMessageType.Register), WampRegisterResponse {
        @Serializable
        public class Details()
    }

    @Serializable
    public data class Invocation(
        val request: Id,
        val registration: Id,
        val details: Details,
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : WampMessage(WampMessageType.Invocation) {
        @Serializable
        public class Details

        init {
            if (argumentsKw != null) requireNotNull(arguments)
        }
    }

    @Serializable
    public data class Yield(
        val request: Id,
        val options: Options,
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : WampMessage(WampMessageType.Yield) {
        @Serializable
        public class Options

        init {
            if (argumentsKw != null) requireNotNull(arguments)
        }
    }

    @Serializable
    public data class InvocationError(
        val request: Id,
        val details: Details,
        val error: WampError,
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : Error(WampMessageType.Invocation) {
        @Serializable
        public class Details

        init {
            if (argumentsKw != null) requireNotNull(arguments)
        }
    }

    @Serializable
    public data class Unregister(
        val request: Id,
        val registration: Id
    ) : WampMessage(WampMessageType.Unregister)

    @Serializable
    public data class Unregistered(
        override val request: Id
    ) : WampMessage(WampMessageType.Unregistered), WampUnregisterResponse

    @Serializable
    public data class UnregisterError(
        override val request: Id,
        val details: Details,
        val error: WampError
    ) : Error(WampMessageType.Unregister), WampUnregisterResponse {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Subscribe(
        val request: Id,
        val options: Options,
        val topic: URI
    ) : WampMessage(WampMessageType.Subscribe) {
        @Serializable
        public class Options
    }

    @Serializable
    public data class Subscribed(
        override val request: Id,
        val subscription: Id
    ) : WampMessage(WampMessageType.Subscribed), WampSubscribeResponse

    @Serializable
    public data class SubscribeError(
        override val request: Id,
        val details: Details,
        val error: WampError
    ) : Error(WampMessageType.Subscribe), WampSubscribeResponse {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Unsubscribe(
        val request: Id,
        val subscription: Id
    ) : WampMessage(WampMessageType.Unsubscribe)

    @Serializable
    public data class Unsubscribed(
        override val request: Id
    ) : WampMessage(WampMessageType.Unsubscribed), WampUnsubscribeResponse

    @Serializable
    public data class UnsubscribeError(
        override val request: Id,
        val details: Details,
        val error: WampError
    ) : Error(WampMessageType.Unsubscribe), WampUnsubscribeResponse {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Publish(
        val request: Id,
        val options: Options,
        val topic: URI,
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ): WampMessage(WampMessageType.Publish) {
        @Serializable
        public class Options(public val acknowledge: Boolean)

        init {
            if (argumentsKw != null) requireNotNull(arguments)
        }
    }

    @Serializable
    public data class Published(
        override val request: Id,
        val publication: Id
    ): WampMessage(WampMessageType.Published), WampPublishResponse

    @Serializable
    public data class PublishError(
        override val request: Id,
        val details: Details,
        val error: WampError
    ): Error(WampMessageType.Publish), WampPublishResponse {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Event(
        val subscription: Id,
        val publication: Id,
        val details: Details,
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ): WampMessage(WampMessageType.Event) {
        @Serializable
        public class Details
    }
}

public sealed interface WampCallResponse {
    public val request: Id
}

public sealed interface WampRegisterResponse {
    public val request: Id
}

public sealed interface WampUnregisterResponse {
    public val request: Id
}

public sealed interface WampSubscribeResponse {
    public val request: Id
}

public sealed interface WampUnsubscribeResponse {
    public val request: Id
}

public sealed interface WampPublishResponse {
    public val request: Id
}

public typealias ArgumentsKw = Map<String, JsonElement>

public typealias Arguments = List<JsonElement>

internal val WampMessageType.errorSerializer
    get() = when (this) {
        WampMessageType.Call -> WampMessage.CallError.serializer()
        WampMessageType.Register -> WampMessage.RegisterError.serializer()
        WampMessageType.Invocation -> WampMessage.InvocationError.serializer()
        WampMessageType.Subscribe -> WampMessage.SubscribeError.serializer()
        WampMessageType.Unsubscribe -> WampMessage.UnsubscribeError.serializer()
        WampMessageType.Publish -> WampMessage.PublishError.serializer()
        else -> error("Missing error for message type: $this")
    }

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal fun <T : Any> T.actualSerializer(klass: KClass<out T>): KSerializer<T> {
    return (Binary.serializersModule.getPolymorphic(klass as KClass<T>, this) ?: klass.serializerOrNull()) as KSerializer<T>
}