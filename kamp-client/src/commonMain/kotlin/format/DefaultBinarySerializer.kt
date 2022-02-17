package net.lostillusion.kamp.client.format

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

public expect class DefaultBinarySerializer<T: Any>(base: KClass<T>) : KSerializer<T>