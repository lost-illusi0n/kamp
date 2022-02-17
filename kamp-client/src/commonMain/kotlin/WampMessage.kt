package net.lostillusion.kamp.client

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
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

            val type = MessageType.from(tree.jsonArray.first().jsonPrimitive.int.toByte())
            val actualSerializer = type.messageSerializer

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
    public data class Error(
        val messageType: MessageType,
        override val request: Id,
        val details: Details,
        val error: WampError,
        val args: Arguments? = null,
        val argumentsKw: ArgumentsKw? = null
    ) : WampMessage(MessageType.Error), WampCallResponse {
        @Serializable
        public class Details
    }
}

public sealed interface WampCallResponse {
    public val request: Id
}

public typealias ArgumentsKw = Map<String, JsonElement>

public typealias Arguments = List<JsonElement>