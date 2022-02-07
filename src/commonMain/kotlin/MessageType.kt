package net.lostillusion.kamp

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MessageType.Serializer::class)
public sealed class MessageType(public val raw: Byte) {
    // Session
    public object Hello : MessageType(1)

    public object Welcome : MessageType(2)

    public object Abort : MessageType(3)

    public object Goodbye : MessageType(6)

    // RPC
    public object Call : MessageType(48)

    public object Result : MessageType(50)

    public object Error : MessageType(8)

    public class Unknown(raw: Byte) : MessageType(raw)

    public companion object {
        public fun from(raw: Byte): MessageType = when (raw.toInt()) {
            1 -> Hello
            2 -> Welcome
            3 -> Abort
            6 -> Goodbye
            8 -> Error
            48 -> Call
            50 -> Result
            else -> Unknown(raw)
        }
    }

    public object Serializer : KSerializer<MessageType> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MessageType", PrimitiveKind.BYTE)

        override fun deserialize(decoder: Decoder): MessageType = from(decoder.decodeByte())

        override fun serialize(encoder: Encoder, value: MessageType) {
            encoder.encodeByte(value.raw)
        }
    }
}

public val MessageType.messageSerializer: KSerializer<out Message>
    get() = when (this) {
        MessageType.Hello -> HelloMessage.serializer()
        MessageType.Welcome -> WelcomeMessage.serializer()
        MessageType.Abort -> AbortMessage.serializer()
        MessageType.Goodbye -> GoodbyeMessage.serializer()
        MessageType.Call -> CallMessage.serializer()
        MessageType.Result -> ResultMessage.serializer()
        MessageType.Error -> ErrorMessage.serializer()
        is MessageType.Unknown -> error("unknown message type encountered: ${this.raw}")
    }
