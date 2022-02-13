package net.lostillusion.kamp.format

import kotlinx.serialization.InheritableSerialInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@Retention
internal annotation class BinaryLength(
    @get:JvmName("binaryLengthBasedSerializer")
    val binarySerializer: KClass<out BinaryLengthBasedSerializer<*, *>>
)
