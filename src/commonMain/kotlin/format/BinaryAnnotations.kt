package net.lostillusion.kamp.format

import kotlinx.serialization.SerialInfo
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
internal annotation class BinaryJsonString

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
internal annotation class BinaryLength(
    @get:JvmName("binaryLengthBasedSerializer")
    val binarySerializer: KClass<out BinaryLengthBasedSerializer<*, *>>
)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
internal annotation class BinaryPadding(val length: Int)