package net.lostillusion.kamp.client.format

import kotlinx.serialization.KSerializer

public interface BinarySize<Self: BinarySize<Self>> {
    public val asInt: Int
    public val serializer: KSerializer<Self>
}