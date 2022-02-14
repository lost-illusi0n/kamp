package net.lostillusion.kamp.transport.raw

import format.BinaryDecoder
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import net.lostillusion.kamp.WampMessageLength
import net.lostillusion.kamp.format.Binary
import net.lostillusion.kamp.transport.WampTransportConnection
import kotlin.coroutines.CoroutineContext
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

    private var remoteMaxLength by Delegates.notNull<Byte>()

    init {
        scope.launch {
            while (isActive && !tcpSocket.isClosed) {
                try {
                    val header = ByteArray(4)
                    incomingChannel.readFully(header, 0, 4)

                    println("received header: ${header.asString}")

                    val data = if (header[0] != WAMP_MAGIC) {
                        val length =
                            BinaryDecoder(header, 0, 1).decodeSerializableValue(WampMessageLength.Serializer).value

                        val payload = ByteArray(length)
                        incomingChannel.readFully(payload, 0, length)

                        println("received payload: ${payload.asString}")

                        header + payload
                    } else {
                        header
                    }

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

        // TODO: proper exceptions
        require(remoteHandshake.serializer == connectionConfig.serializerType) { "Server responded with a different serializer!" }

        remoteMaxLength = remoteHandshake.length
    }

    public suspend fun send(packet: WampRawTransportPacket) {
        val frame = Binary.encodeToByteArray(WampRawTransportPacket.Serializer, packet)
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