package net.lostillusion.kamp.client.transport.raw

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import mu.KotlinLogging
import net.lostillusion.kamp.client.WampMessageLength
import net.lostillusion.kamp.client.format.Binary
import net.lostillusion.kamp.client.format.BinaryDecoder

private sealed class MaterializedValue<out T> {
    class Value<T>(val value: T) : MaterializedValue<T>()
    class Error(val exception: Throwable) : MaterializedValue<Nothing>()
}

private fun <T> Flow<T>.materialize(): Flow<MaterializedValue<T>> =
    map<T, MaterializedValue<T>> { MaterializedValue.Value(it) }.catch { emit(MaterializedValue.Error(it)) }

private fun <T> Flow<MaterializedValue<T>>.dematerialize(): Flow<T> =
    map {
        when (it) {
            is MaterializedValue.Value -> it.value
            is MaterializedValue.Error -> {
                throw it.exception
            }
        }
    }

private fun <T> Flow<MaterializedValue<T>>.values(): Flow<T> =
    filterIsInstance<MaterializedValue.Value<T>>().map { it.value }

private val packetLogger = KotlinLogging.logger { }

@OptIn(ExperimentalCoroutinesApi::class)
internal fun WampRawTransportConnection.rawFrameFlow(): Pair<Flow<WampRawTransportPacket>, SharedFlow<WampRawTransportPacket>> {
    val buffer = ByteArray(localMaxLengthInBytes)
    val headerDecoder = BinaryDecoder(buffer, 0, 1)

    val base = channelFlow {
        while (isActive) {
            if (incoming.isClosedForRead) throw WampTransportClosedException(
                remoteAddress,
                "The incoming ByteReadChannel was closed!"
            )
            incoming.readFully(buffer, 0, 4)

            val data = if (buffer[0] != WAMP_MAGIC) {
                headerDecoder.cursor = 1
                val length = headerDecoder.decodeSerializableValue(WampMessageLength.Serializer).value

                if (length > localMaxLengthInBytes) {
                    throw WampRawTransportProtocolViolationException(
                        remoteAddress,
                        "Peer sent packet larger than our requested max length! Received: $length bytes | Expected (maximum): $localMaxLengthInBytes bytes"
                    )
                }

                incoming.readFully(buffer, 4, length)

                buffer.copyOf(4 + length)
            } else {
                buffer.copyOf(4)
            }

            send(data)
        }
    }.map { Binary.decodeFromByteArray(WampRawTransportPacket.Serializer, it) }
        .onEach { packetLogger.trace { "TRANSPORT <<< $it" } }
        .materialize()
        .shareIn(scope, SharingStarted.Eagerly)

    val main = base.dematerialize()
    val internal = base.values().shareIn(scope, SharingStarted.Eagerly)

    return Pair(main, internal)
}