package net.lostillusion.kamp.transport.raw

import net.lostillusion.kamp.WampSerializerType

private const val MAX_LENGTH: UByte = 15u

public data class WampRawTransportConnectionConfig(
    val maxLength: UByte,
    val serializerType: WampSerializerType,
    val handshakeTimeout: Long,
    val pongTimeout: Long
) {
    init {
        require(maxLength in 0.toUByte()..MAX_LENGTH)
    }
}

internal fun WampRawTransportConnectionConfig.intoHandshake(): WampRawTransportPacket.Handshake {
    require(serializerType == WampSerializerType.Json) { "Only the JSON serializer type is currently supported!" }
    return WampRawTransportPacket.Handshake(maxLength, serializerType)
}

public class WampRawTransportConnectionConfigBuilder {
    public var maxLength: UByte = MAX_LENGTH
    public var serializerType: WampSerializerType = WampSerializerType.Json
    public var handshakeTimeout: Long = 1000
    public var pongTimeout: Long = 1000

    public fun build(): WampRawTransportConnectionConfig =
        WampRawTransportConnectionConfig(maxLength, serializerType, handshakeTimeout, pongTimeout)
}