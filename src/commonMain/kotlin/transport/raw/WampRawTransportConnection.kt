package net.lostillusion.kamp.transport.raw

import format.BinaryDecoder
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.*
import net.lostillusion.kamp.*
import net.lostillusion.kamp.format.Binary
import net.lostillusion.kamp.transport.WampTransportConnection
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow

public class WampRawTransportConnection(
    internal val tcpSocket: Socket,
    private val connectionConfig: WampRawTransportConnectionConfig,
    coroutineContext: CoroutineContext
) : WampTransportConnection {
    public val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext + CoroutineName("wamp-raw-transport-connection"))

    public override val remoteAddress: NetworkAddress = tcpSocket.remoteAddress

    private val _incoming: MutableSharedFlow<WampRawTransportPacket> = MutableSharedFlow()
    public val incoming: SharedFlow<WampRawTransportPacket> = _incoming

    private val outgoingChannel = tcpSocket.openWriteChannel()

    // 4 should be enough for the header until we establish this
    private var remoteMaxLength: UByte = 4u
    private val remoteMaxLengthInBytes
        get() = 2f.pow(remoteMaxLength.toInt() + 9).toInt()

    internal val localMaxLengthInBytes = 2f.pow(connectionConfig.maxLength.toInt() + 9).toInt()

    init {
        PingPong(this)
        SocketReader(tcpSocket.openReadChannel(), _incoming, this)
        scope.launch {
            incoming.filterIsInstance<WampMessage.Goodbye>().onEach {
                send(WampRawTransportPacket.Frame.Message(WampMessage.Goodbye(reason = WampClose.GoodbyeAndOut)))
                close()
            }
        }
    }

    internal suspend fun handshake() {
        send(connectionConfig.intoHandshake())

        val remoteHandshake = withTimeoutOrNull(connectionConfig.handshakeTimeout) {
            incoming.filterIsInstance<WampRawTransportPacket.Handshake>().first()
        } ?: throw WampTimeout(
            remoteAddress,
            "Timed out waiting ${connectionConfig.handshakeTimeout}ms for a handshake back!"
        )

        if (remoteHandshake.serializer != connectionConfig.serializerType) {
            close()

            throw WampRawTransportProtocolViolationException(
                remoteAddress,
                "Sent serializer of type ${connectionConfig.serializerType} but received ${remoteHandshake.serializer} instead!"
            )
        }

        remoteMaxLength = remoteHandshake.length
    }

    public suspend fun ping(data: ByteArray = byteArrayOf(69)) {
        send(WampRawTransportPacket.Frame.Ping(data))

        val pong = withTimeoutOrNull(connectionConfig.pongTimeout) {
            incoming.filterIsInstance<WampRawTransportPacket.Frame.Pong>().first()
        } ?: throw WampTimeout(remoteAddress, "Timed out waiting ${connectionConfig.pongTimeout}ms for a pong back!")

        if (!pong.payload.contentEquals(data)) {
            close()

            throw WampRawTransportProtocolViolationException(
                remoteAddress,
                "Ping sent payload: ${data.asString} but pong returned different payload!: ${pong.payload.asString}!"
            )
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

private class SocketReader(
    incoming: ByteReadChannel,
    output: MutableSharedFlow<WampRawTransportPacket>,
    connection: WampRawTransportConnection
) {
    init {
        connection.scope.launch {
            val buffer = ByteArray(connection.localMaxLengthInBytes)
            val headerDecoder = BinaryDecoder(buffer, 0, 1)

            while (isActive && !connection.tcpSocket.isClosed) {
                try {
                    headerDecoder.cursor = 1
                    incoming.readFully(buffer, 0, 4)

                    val data = if (buffer[0] != WAMP_MAGIC) {
                        val length = headerDecoder.decodeSerializableValue(WampMessageLength.Serializer).value

                        if (length > connection.localMaxLengthInBytes) {
                            connection.close()

                            throw WampRawTransportProtocolViolationException(
                                connection.remoteAddress,
                                "Peer sent packet larger than our requested max length! Received: $length bytes | Expected (maximum): ${connection.localMaxLengthInBytes} bytes"
                            )
                        }

                        incoming.readFully(buffer, 4, length)

                        buffer.copyOf(4 + length)
                    } else {
                        buffer.copyOf(4)
                    }

                    println("received: ${data.asString}")

                    val packet = Binary.decodeFromByteArray(WampRawTransportPacket.Serializer, data)

                    println("received: $packet")

                    output.emit(packet)
                } catch (e: ClosedReceiveChannelException) {
                    throw WampTransportClosedException(connection.remoteAddress)
                }
            }
        }
    }
}

public class WampRawTransportProtocolViolationException(host: NetworkAddress, message: String) :
    WampTransportException(host, message)

public class WampTransportClosedException(host: NetworkAddress) :
    WampTransportException(host, "The WAMP transport connection for $host has closed!")

private val ByteArray.asString
    get() = joinToString(" ") { it.toUByte().toString(2).padStart(8, '0') }