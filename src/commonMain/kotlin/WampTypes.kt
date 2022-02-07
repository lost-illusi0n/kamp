package net.lostillusion.kamp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.pow
import kotlin.random.Random

@Serializable
@JvmInline
public value class Id private constructor(public val value: Long) {
    public companion object {
        public fun generateGlobal(): Id = Id(Random.nextLong(1, 2.toDouble().pow(53).toLong()))
        public fun generateRouter(): Id = Id(Random.nextInt().toLong())
//        public fun generateSession()
    }
}

@Serializable
@JvmInline
public value class Length private constructor(public val value: Int) {
    public companion object {
        public operator fun invoke(value: Int): Length {
            require(value shr 24 == 0)
            return Length(value)
        }
    }

    public operator fun component0(): Byte = (value shr 16).toByte()
    public operator fun component1(): Byte = (value shr 8).toByte()
    public operator fun component2(): Byte = value.toByte()
}