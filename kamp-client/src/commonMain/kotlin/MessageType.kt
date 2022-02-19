package net.lostillusion.kamp.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MessageType.Serializer::class)
public enum class MessageType(public val raw: Byte) {
    // Session
    Hello(1),
    Welcome(2),
    Abort(3),
    Goodbye(6),
    Error(8),

    // RPC
    Call(48),
    Result(50),
    Register(64),
    Registered(65),
    Unregister(66),
    Unregistered(67),
    Invocation(68),
    Yield(70);

    public companion object {
        public fun from(raw: Byte): MessageType = values().find { it.raw == raw } ?: error("unknown message type: $raw")
    }

    public object Serializer : KSerializer<MessageType> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MessageType", PrimitiveKind.BYTE)

        override fun deserialize(decoder: Decoder): MessageType = from(decoder.decodeByte())

        override fun serialize(encoder: Encoder, value: MessageType) {
            encoder.encodeByte(value.raw)
        }
    }
}

public val MessageType.messageSerializer: KSerializer<out WampMessage>
    get() = when (this) {
        MessageType.Hello -> WampMessage.Hello.serializer()
        MessageType.Welcome -> WampMessage.Welcome.serializer()
        MessageType.Abort -> WampMessage.Abort.serializer()
        MessageType.Goodbye -> WampMessage.Goodbye.serializer()
        MessageType.Error -> WampMessage.Error.Serializer
        MessageType.Call -> WampMessage.Call.serializer()
        MessageType.Result -> WampMessage.Result.serializer()
        MessageType.Register -> WampMessage.Register.serializer()
        MessageType.Registered -> WampMessage.Registered.serializer()
        MessageType.Unregister -> WampMessage.Unregister.serializer()
        MessageType.Unregistered -> WampMessage.Unregistered.serializer()
        MessageType.Invocation -> WampMessage.Invocation.serializer()
        MessageType.Yield -> WampMessage.Yield.serializer()
    }
