package net.lostillusion.kamp

import io.ktor.utils.io.core.*

public data class Header(val frameType: FrameType, val length: Length)

internal fun ByteReadPacket.parseHeader(): Header {
    require(remaining == 4L)

    // the first 5 bits before the frame type are reserved and should be zero.
    // if this changes, we cannot assume readByte() will solely be the frameType
    val frame = FrameType.from(readByte())

    var lengthValue = 0

    repeat(3) {
        lengthValue = (lengthValue shl 8) or readByte().toInt()
    }

    return Header(frame, Length(lengthValue))
}