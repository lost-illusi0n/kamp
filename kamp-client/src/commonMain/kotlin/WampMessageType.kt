package net.lostillusion.kamp.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WampMessageType.Serializer::class)
public enum class WampMessageType(public val raw: Byte) {
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
        public fun from(raw: Byte): WampMessageType = values().find { it.raw == raw } ?: error("unknown message type: $raw")
    }

    public object Serializer : KSerializer<WampMessageType> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MessageType", PrimitiveKind.BYTE)

        override fun deserialize(decoder: Decoder): WampMessageType = from(decoder.decodeByte())

        override fun serialize(encoder: Encoder, value: WampMessageType) {
            encoder.encodeByte(value.raw)
        }
    }
}

public val WampMessageType.messageSerializer: KSerializer<out WampMessage>
    get() = when (this) {
        WampMessageType.Hello -> WampMessage.Hello.serializer()
        WampMessageType.Welcome -> WampMessage.Welcome.serializer()
        WampMessageType.Abort -> WampMessage.Abort.serializer()
        WampMessageType.Goodbye -> WampMessage.Goodbye.serializer()
        WampMessageType.Error -> WampMessage.Error.Serializer
        WampMessageType.Call -> WampMessage.Call.serializer()
        WampMessageType.Result -> WampMessage.Result.serializer()
        WampMessageType.Register -> WampMessage.Register.serializer()
        WampMessageType.Registered -> WampMessage.Registered.serializer()
        WampMessageType.Unregister -> WampMessage.Unregister.serializer()
        WampMessageType.Unregistered -> WampMessage.Unregistered.serializer()
        WampMessageType.Invocation -> WampMessage.Invocation.serializer()
        WampMessageType.Yield -> WampMessage.Yield.serializer()
    }
