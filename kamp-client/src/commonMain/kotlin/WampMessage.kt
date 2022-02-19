package net.lostillusion.kamp.client

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

// TODO: open up for extension messages

@OptIn(ExperimentalSerializationApi::class)
@Serializable
public sealed class WampMessage(public val type: MessageType) {
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

            val type = MessageType.from(tree.jsonArray[0].jsonPrimitive.int.toByte())
            val actualSerializer = if (type != MessageType.Error) {
                type.messageSerializer
            } else {
                val call = MessageType.from(tree.jsonArray[1].jsonPrimitive.int.toByte())

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
    public data class Hello(val realm: String, val details: Details) : WampMessage(MessageType.Hello) {
        @Serializable
        public data class Details(val agent: String? = null, val roles: Map<Role, JsonObject>)
    }

    @Serializable
    public data class Welcome(val session: Id, val details: Details) : WampMessage(MessageType.Welcome) {
        @Serializable
        public data class Details(val agent: String? = null, val roles: Map<Role, JsonObject>)
    }

    @Serializable
// TODO: make a uri type for reason and other similar fields
    public data class Abort(val details: Details, val reason: String) : WampMessage(MessageType.Abort) {
        @Serializable
        public data class Details(val message: String)
    }

    @Serializable
    public data class Goodbye(val details: Details = Details(), val reason: WampClose) : WampMessage(MessageType.Goodbye) {
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
    ) : WampMessage(MessageType.Call) {
        @Serializable
        public class Options
    }

    @Serializable
    public data class Result(
        override val request: Id,
        val details: Details,
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : WampMessage(MessageType.Result), WampCallResponse {
        @Serializable
        public class Details
    }

    @Serializable
    public sealed class Error(public val messageType: MessageType) : WampMessage(MessageType.Error) {
        public object Serializer : KSerializer<Error> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Error") {
                element("messageType", MessageType.Serializer.descriptor)
            }

            override fun deserialize(decoder: Decoder): Error {
                return decoder.decodeSerializableValue(MessageType.Serializer).errorSerializer.deserialize(decoder)
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
    ) : Error(MessageType.Call), WampCallResponse {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Register(
        val request: Id,
        val options: Details,
        val procedure: URI
    ) : WampMessage(MessageType.Register) {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Registered(
        override val request: Id,
        val registration: Id
    ) : WampMessage(MessageType.Registered), WampRegisterResponse

    @Serializable
    public data class RegisterError(
        override val request: Id,
        val details: Details,
        val error: WampError
    ) : Error(MessageType.Register), WampRegisterResponse {
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
    ) : WampMessage(MessageType.Invocation) {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Yield(
        val request: Id,
        val options: Options,
        val arguments: Arguments,
        val argumentsKw: ArgumentsKw
    ) : WampMessage(MessageType.Yield) {
        @Serializable
        public class Options
    }

    @Serializable
    public data class InvocationError(
        val request: Id,
        val details: Details,
        val error: WampError,
        val arguments: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : Error(MessageType.Invocation) {
        @Serializable
        public class Details
    }

    @Serializable
    public data class Unregister(
        val request: Id,
        val registration: Id
    ) : WampMessage(MessageType.Unregister)

    @Serializable
    public data class Unregistered(
        override val request: Id
    ) : WampMessage(MessageType.Unregistered), WampUnregisterResponse

    @Serializable
    public data class UnregisterError(
        override val request: Id,
        val details: Details,
        val error: WampError
    ) : Error(MessageType.Unregister), WampUnregisterResponse {
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

public typealias ArgumentsKw = Map<String, JsonElement>

public typealias Arguments = List<JsonElement>

internal val MessageType.errorSerializer
    get() = when (this) {
        MessageType.Call -> WampMessage.CallError.serializer()
        MessageType.Register -> WampMessage.RegisterError.serializer()
        MessageType.Invocation -> WampMessage.InvocationError.serializer()
        else -> error("Missing error for message type: $this")
    }