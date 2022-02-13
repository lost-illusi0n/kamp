package net.lostillusion.kamp.format

import kotlinx.serialization.SerialInfo

/**
 * A value marked by this annotation will first be serialized as a JSON string and the resulting string will be encoded as a ByteArray.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@Retention
internal annotation class BinaryJsonString
