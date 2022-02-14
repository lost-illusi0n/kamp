package net.lostillusion.kamp.transport.raw

import net.lostillusion.kamp.WampSerializerType

private const val MAX_LENGTH: Byte = 15

public data class WampRawTransportConnectionConfig(
    val maxLength: Byte,
    val serializerType: WampSerializerType,
    val handshakeTimeout: Long,
) {
    init {
        require(maxLength in 0..MAX_LENGTH)
    }
}

internal fun WampRawTransportConnectionConfig.intoHandshake(): WampRawTransportPacket.Handshake {
    require(serializerType == WampSerializerType.Json) { "Only the JSON serializer type is currently supported!" }
    return WampRawTransportPacket.Handshake(maxLength, serializerType)
}

public class WampRawTransportConnectionConfigBuilder {
    public var maxLength: Byte = MAX_LENGTH
    public var serializerType: WampSerializerType = WampSerializerType.Json
    public var handshakeTimeout: Long = 1000

    public fun build(): WampRawTransportConnectionConfig =
        WampRawTransportConnectionConfig(maxLength, serializerType, handshakeTimeout)
}