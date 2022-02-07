package net.lostillusion.kamp

import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.experimental.and

// TODO: open up for extension messages

@OptIn(ExperimentalSerializationApi::class)
@Serializable
public sealed class Message(public val type: MessageType) {
    public object Serializer : KSerializer<Message> {
        override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

        @Suppress("UNCHECKED_CAST")
        @OptIn(InternalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: Message) {
            val output = encoder as JsonEncoder

            val actualSerializer = (
                encoder.serializersModule.getPolymorphic(Message::class, value)
                    ?: value::class.serializer()
                ) as KSerializer<Message>

            val element = output.json.encodeToJsonElement(actualSerializer, value).jsonObject

            output.encodeJsonElement(JsonArray(element.entries.map { it.value }))
        }

        override fun deserialize(decoder: Decoder): Message {
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

    // todo: fuck off, make proper frames
    public fun encodeToRawFrame(): ByteArray {
        val serialized: ByteArray = Json.encodeToString(Serializer, this).toByteArray()

        val frame = BytePacketBuilder().apply {
            // RRR RTTT
            require(FrameType.Regular.raw and ((0xFF shl 3).toByte()) == 0.toByte())
            writeByte(FrameType.Regular.raw)

            // 3 byte length field
            writeByte((serialized.size shr 16).toByte())
            writeByte((serialized.size shr 8).toByte())
            writeByte(serialized.size.toByte())

            writeFully(serialized)
        }

        return frame.build().readBytes()
    }
}

@Serializable
public data class HelloMessage(val realm: String, val details: Details) : Message(MessageType.Hello) {
    @Serializable
    public data class Details(val agent: String? = null, val roles: Map<Role, JsonObject>)
}

@Serializable
public data class WelcomeMessage(val session: Id, val details: Details) : Message(MessageType.Welcome) {
    @Serializable
    public data class Details(val agent: String? = null, val roles: Map<Role, JsonObject>)
}

@Serializable
// TODO: make a uri type for reason and other similar fields
public data class AbortMessage(val details: Details, val reason: String) : Message(MessageType.Abort) {
    @Serializable
    public data class Details(val message: String)
}

@Serializable
public data class GoodbyeMessage(val details: Details, val reason: String) : Message(MessageType.Goodbye) {
    @Serializable
    public data class Details(val message: String)
}

public typealias Arguments = List<@Polymorphic Any>

public typealias ArgumentsKw = Map<String, @Polymorphic Any>

@Serializable
public data class CallMessage(
    val request: Id,
    val options: Options,
    val procedure: String,
    // these should not be present in serialized form if null
    val arguments: Arguments? = null,
    val argumentsKw: ArgumentsKw? = null
) : Message(MessageType.Call) {
    @Serializable
    public class Options
}

@Serializable
public data class ResultMessage(
    val request: Id,
    val details: Details,
    val arguments: Arguments? = null,
    val argumentsKw: ArgumentsKw? = null
) : Message(MessageType.Result) {
    @Serializable
    public class Details
}

@Serializable
public data class ErrorMessage(
    val messageType: MessageType,
    val request: Id,
    val details: Details,
    val error: WampError
) : Message(MessageType.Error) {
    @Serializable
    public class Details
}