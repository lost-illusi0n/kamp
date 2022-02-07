package net.lostillusion.kamp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private const val WAMP_MAGIC: Byte = 0x7F
private const val MAX_LENGTH: UByte = 15u

private val HANDSHAKE = byteArrayOf(
    WAMP_MAGIC,
    ((MAX_LENGTH.toInt() shl 4) or JSON_SERIALIZER_TYPE.toInt()).toByte(),
    0,
    0
)

public class DefaultSession(public val data: DefaultSessionData) : Session {
    public val scope: CoroutineScope = CoroutineScope(SupervisorJob() + CoroutineName("kamp-session"))

    private lateinit var outgoing: ByteWriteChannel

    override val incoming: SharedFlow<Message> = data.messageFlow

    private val json = Json { ignoreUnknownKeys = true }

    init {
        incoming.filterIsInstance<WelcomeMessage>()
            .onEach { println("we are welcome") }
            .launchIn(scope)
    }

    @OptIn(InternalAPI::class)
    override suspend fun connect(config: SessionConfig): Unit = withContext(scope.coroutineContext) {
        val socket = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(data.address)

        println("connected")

        val incoming = socket.openReadChannel()
        outgoing = socket.openWriteChannel()

        println("opened. initiating handshake")

        handshake(incoming)

        println("finished handshake.")

        val hello = HelloMessage(
            realm = config.realm,
            details = HelloMessage.Details(
                config.agent,
                config.roles.associateWith { JsonObject(emptyMap()) }
            )
        )

        send(hello)

        println("sent hello. started listening")

        while (isActive) {
            val header = incoming.readPacket(4).parseHeader()

            println(header)

            when (header.frameType) {
                FrameType.Regular -> {
                    val message = json.decodeFromString(
                        deserializer = Message.Serializer,
                        string = incoming.readPacket(header.length.value).readText()
                    )

                    println(message)

                    data.messageFlow.emit(message)
                }
                FrameType.Ping -> TODO()
                FrameType.Pong -> TODO()
                is FrameType.Unknown -> error("unknown frame received: ${header.frameType.raw}")
            }
        }
    }

    private suspend fun handshake(incoming: ByteReadChannel) {
        outgoing.writeFully(HANDSHAKE, 0, HANDSHAKE.size)
        outgoing.flush()

        println("sent client handshake: ${HANDSHAKE.joinToString(" ") { it.toUByte().toString(2).padStart(8, '0') } }")

        with(incoming.readPacket(4)) {
            println("received server handshake")
            require(readByte() == WAMP_MAGIC)
            val secondOctet = readByte().toUByte()

            // TODO: take account of the length sent by the router
            val len = (secondOctet.toInt() shr 4).toUByte()
            val serializer = secondOctet and 0x0Fu

            require(serializer == JSON_SERIALIZER_TYPE)

            discard()
        }
    }

    override suspend fun send(message: Message) {
        val frame = message.encodeToRawFrame()
        outgoing.writeFully(frame, 0, frame.size)
        outgoing.flush()
    }
}