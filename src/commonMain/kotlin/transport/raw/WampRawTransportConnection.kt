package net.lostillusion.kamp.transport.raw

import io.ktor.network.sockets.*
import io.ktor.util.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import net.lostillusion.kamp.WampTimeout
import net.lostillusion.kamp.WampTransportException
import net.lostillusion.kamp.format.Binary
import net.lostillusion.kamp.transport.WampTransportConnection
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow

private val logger = KotlinLogging.logger { }

public class WampRawTransportConnection(
    internal val tcpSocket: Socket,
    private val connectionConfig: WampRawTransportConnectionConfig,
    coroutineContext: CoroutineContext
) : WampTransportConnection {
    public val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext + CoroutineName("wamp-raw-transport-connection"))

    public override val remoteAddress: NetworkAddress = tcpSocket.remoteAddress

    internal val incoming = tcpSocket.openReadChannel()
    internal val outgoing = tcpSocket.openWriteChannel()

    // 4 should be enough for the header until we establish this
    private var remoteMaxLength: UByte = 4u
    private val remoteMaxLengthInBytes
        get() = 2f.pow(remoteMaxLength.toInt() + 9).toInt()

    internal val localMaxLengthInBytes = 2f.pow(connectionConfig.maxLength.toInt() + 9).toInt()

    public val incomingPackets: Flow<WampRawTransportPacket>
    private val _incomingPackets: SharedFlow<WampRawTransportPacket>

    init {
        /**
         * main is our user-facing "main" collector. here exceptions are propagated and should only have one collector
         * internal is our internal collector. all other collectors use this and contains no exceptions, beware.
         */
        val (main, internal) = rawFrameFlow()

        incomingPackets = main
        _incomingPackets = internal

        pingPong()
    }

    private fun WampRawTransportConnection.pingPong() {
        _incomingPackets
            .filterIsInstance<WampRawTransportPacket.Frame.Ping>()
            .onEach { send(WampRawTransportPacket.Frame.Pong(it.payload)) }
            .launchIn(scope)
    }

    internal suspend fun handshake() {
        send(connectionConfig.intoHandshake())

        val remoteHandshake = withTimeoutOrNull(connectionConfig.handshakeTimeout) {
            _incomingPackets.filterIsInstance<WampRawTransportPacket.Handshake>().first()
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
            _incomingPackets.filterIsInstance<WampRawTransportPacket.Frame.Pong>().first()
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
            logger.warn { "Dropping a packet of length ${frame.size} because it is too big!\n$packet" }
            return
        }

        logger.trace { "TRANSPORT >>> $packet" }
        outgoing.writeFully(frame, 0, frame.size)
        outgoing.flush()
    }

    public suspend fun close() {
        tcpSocket.close()
        tcpSocket.awaitClosed()
        scope.cancel()
    }
}

public class WampRawTransportProtocolViolationException(host: NetworkAddress, message: String) :
    WampTransportException(host, message)

public class WampRawTransportErrorFrameReceived(host: NetworkAddress, public val error: WampRawTransportError) :
    WampTransportException(host, "WAMP raw transport received an error frame! $error")

public class WampTransportClosedException(
    host: NetworkAddress,
    message: String = "The WAMP transport connection for $host has closed!"
) : WampTransportException(host, message)

private val ByteArray.asString
    get() = joinToString(" ") { it.toUByte().toString(2).padStart(8, '0') }