package net.lostillusion.kamp.transport.raw

import format.BinaryDecoder
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import net.lostillusion.kamp.WampMessageLength
import net.lostillusion.kamp.format.Binary
import net.lostillusion.kamp.transport.WampTransportConnection
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow
import kotlin.properties.Delegates

public class WampRawTransportConnection(
    private val tcpSocket: Socket,
    private val connectionConfig: WampRawTransportConnectionConfig,
    coroutineContext: CoroutineContext
) : WampTransportConnection {
    public val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext + CoroutineName("wamp-raw-transport-connection"))

    public override val remoteAddress: NetworkAddress = tcpSocket.remoteAddress

    private val _incoming: MutableSharedFlow<WampRawTransportPacket> = MutableSharedFlow()
    public val incoming: SharedFlow<WampRawTransportPacket> = _incoming

    private val incomingChannel = tcpSocket.openReadChannel()
    private val outgoingChannel = tcpSocket.openWriteChannel()

    // 4 should be enough for the header until we establish this
    private var remoteMaxLength: UByte = 4u
    private val remoteMaxLengthInBytes
        get() = 2f.pow(remoteMaxLength.toInt() + 9).toInt()

    private val localMaxLengthInBytes = 2f.pow(connectionConfig.maxLength.toInt() + 9).toInt()

    init {
        PingPong(this)

        scope.launch {
            val buffer = ByteArray(localMaxLengthInBytes)
            val headerDecoder = BinaryDecoder(buffer, 0, 1)

            while (isActive && !tcpSocket.isClosed) {
                try {
                    headerDecoder.cursor = 1
                    incomingChannel.readFully(buffer, 0, 4)

                    val data = if (buffer[0] != WAMP_MAGIC) {
                        val length = headerDecoder.decodeSerializableValue(WampMessageLength.Serializer).value

                        if (length > localMaxLengthInBytes) {
                            close()
                            // TODO: upstream
                        }

                        incomingChannel.readFully(buffer, 4, length)

                        buffer.copyOf(4 + length)
                    } else {
                        buffer.copyOf(4)
                    }

                    println("received: ${data.asString}")

                    val packet = Binary.decodeFromByteArray(WampRawTransportPacket.Serializer, data)

                    println("received: $packet")

                    _incoming.emit(packet)
                } catch (e: ClosedReceiveChannelException) {
                    // TODO: send this upstream
                }
            }
        }
    }

    internal suspend fun handshake() {
        send(connectionConfig.intoHandshake())

        val remoteHandshake = withTimeout(connectionConfig.handshakeTimeout) {
            incoming.filterIsInstance<WampRawTransportPacket.Handshake>().first()
        }

        if (remoteHandshake.serializer != connectionConfig.serializerType) {
            println("weird")
            close()

            // TODO: propagate closed connection
        }

        remoteMaxLength = remoteHandshake.length
    }

    public suspend fun ping(data: ByteArray = byteArrayOf(69)) {
        send(WampRawTransportPacket.Frame.Ping(data))

        val pong = withTimeout(connectionConfig.pongTimeout) {
            incoming.filterIsInstance<WampRawTransportPacket.Frame.Pong>().first()
        }

        if (!pong.payload.contentEquals(data)) {
            close()

            // TODO: propagate
        }
    }

    public suspend fun send(packet: WampRawTransportPacket) {
        val frame = Binary.encodeToByteArray(WampRawTransportPacket.Serializer, packet)

        if (frame.size > remoteMaxLengthInBytes) {
            println("dropping a packet too large!: $packet")
            return
        }

        println("sending: ${frame.asString}")
        outgoingChannel.writeFully(frame, 0, frame.size)
        outgoingChannel.flush()
    }

    public suspend fun close() {
        tcpSocket.close()
        tcpSocket.awaitClosed()
        scope.cancel()
    }
}

private val ByteArray.asString
    get() = joinToString(" ") { it.toUByte().toString(2).padStart(8, '0') }