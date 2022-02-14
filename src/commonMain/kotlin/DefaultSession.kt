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
import net.lostillusion.kamp.format.Binary
import net.lostillusion.kamp.transport.WampTransportSocket
import net.lostillusion.kamp.transport.raw.WampRawTransportPacket

private const val WAMP_MAGIC: Byte = 0x7F
private const val MAX_LENGTH: UByte = 15u

public class DefaultSession(
    public val data: DefaultSessionData,
    public val socket: WampTransportSocket
) : Session {
    public val scope: CoroutineScope = CoroutineScope(SupervisorJob() + CoroutineName("kamp-session"))

    private lateinit var outgoing: ByteWriteChannel

    override val incoming: SharedFlow<WampMessage> = data.messageFlow

    private val json = Json { ignoreUnknownKeys = true }

    init {
        incoming.filterIsInstance<WelcomeWampMessage>()
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

        val hello = HelloWampMessage(
            realm = config.realm,
            details = HelloWampMessage.Details(
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
                        deserializer = WampMessage,
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
        // TODO: abstract socket
        val sendingOctet = ((MAX_LENGTH.toInt() shl 4) or WampSerializerType.Json.raw.toInt()).toByte()
        val handshake = byteArrayOf(WAMP_MAGIC, sendingOctet, 0, 0)
        outgoing.writeFully(handshake, 0, handshake.size)
        outgoing.flush()
        println("sent client handshake: ${handshake.joinToString(" ") { it.toUByte().toString(2).padStart(8, '0') } }")

        with(incoming.readPacket(4)) {
            println("received server handshake")
            require(readByte() == WAMP_MAGIC)
            val secondOctet = readByte().toUByte()

            // TODO: take account of the length sent by the router
            val len = (secondOctet.toInt() shr 4).toUByte()
            val serializer = secondOctet and 0x0Fu

//            require(serializer == JSON_SERIALIZER_TYPE)

            discard()
        }
    }

    override suspend fun send(message: WampMessage) {
        socket.send(message)
        val packet = WampRawTransportPacket.Frame.Message(message)
        val frame = Binary.encodeToByteArray(WampRawTransportPacket.Serializer, packet)
        outgoing.writeFully(frame, 0, frame.size)
        outgoing.flush()
    }
}