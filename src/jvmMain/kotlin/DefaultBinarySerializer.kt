package net.lostillusion.kamp.format

import io.ktor.util.reflect.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

private val json = Json { ignoreUnknownKeys = true }

// we need reflection for now
// the reason for a custom serializer is to allow for
// variable-sized lengths for collections. by default
// only a int is used however we need more flexibility
public actual class DefaultBinarySerializer<T : Any> actual constructor(private val base: KClass<T>) : KSerializer<T> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(base.simpleName.toString()) {
        fun byte(param: KProperty1<T, *>) {
            element(param.name, Byte.serializer().descriptor)
        }

        fun collection(param: KProperty1<T, *>) {
            val length = param.findAnnotation<BinaryLength>()!!
            val binarySerializer = length.binarySerializer.objectInstance!!

            element("${param.name}Length", binarySerializer.sizeSerializer.descriptor)
            element(param.name, ByteArraySerializer().descriptor)
        }

        fun serializable(param: KProperty1<T, *>) {
            if (param.hasAnnotation<BinaryJsonString>()) {
                val length = param.findAnnotation<BinaryLength>()!!
                val binarySerializer = length.binarySerializer.objectInstance!!

                element("length", binarySerializer.sizeSerializer.descriptor)
                element(param.name, ByteArraySerializer().descriptor)

                return
            }

            element(param.name, param.returnType.jvmErasure.serializer().descriptor)
        }

        base.declaredMemberProperties.reversed().forEach {
            when (it.returnType) {
                Byte::class.starProjectedType -> byte(it)
                ByteArray::class.starProjectedType, String::class.starProjectedType -> collection(it)
                else -> serializable(it)
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    override fun deserialize(decoder: Decoder): T {
        val primary = base.primaryConstructor!!
        val values = mutableListOf<Any?>()

        base.declaredMemberProperties.reversed().forEach {
            values += when (it.returnType) {
                Byte::class.starProjectedType -> decoder.decodeByte()
                ByteArray::class.starProjectedType, String::class.starProjectedType -> {
                    val length = it.findAnnotation<BinaryLength>()!!

                    length.binarySerializer.objectInstance!!.deserialize(decoder)
                }
                else -> {
                    if (it.hasAnnotation<BinaryJsonString>()) {
                        val length = it.findAnnotation<BinaryLength>()!!

                        val text = length.binarySerializer.objectInstance!!.deserialize(decoder) as String
                        json.decodeFromString(it.returnType.jvmErasure.serializer, text)
                    } else {
                        it.returnType.jvmErasure.serializer().deserialize(decoder)
                    }
                }
            }
        }

        return primary.call(*values.toTypedArray())
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: T) {
        base.declaredMemberProperties.reversed().forEach {
            val property = it.get(value) as Any

            when (it.returnType) {
                Byte::class.starProjectedType -> encoder.encodeByte(it.get(value) as Byte)
                ByteArray::class.starProjectedType -> {
                    val length = it.findAnnotation<BinaryLength>()!!

                    (length.binarySerializer.objectInstance!! as KSerializer<ByteArray>)
                        .serialize(encoder, property as ByteArray)
                }
                String::class.starProjectedType -> {
                    val length = it.findAnnotation<BinaryLength>()!!

                    (length.binarySerializer.objectInstance!! as KSerializer<String>)
                        .serialize(encoder, property as String)
                }
                else -> {
                    val serializer = (it.findAnnotation<Serializable>()?.with?.objectInstance
                        ?: it.returnType.jvmErasure.serializer()) as KSerializer<Any>

                    if (it.hasAnnotation<BinaryJsonString>()) {
                        val binarySerializer =
                            it.findAnnotation<BinaryLength>()!!.binarySerializer.objectInstance!! as KSerializer<String>

                        val text = json.encodeToString((it.returnType.jvmErasure as KClass<Any>).serializer, property)
                        binarySerializer.serialize(encoder, text)
                    } else {
                        serializer.serialize(encoder, property)
                    }
                }
            }
        }
    }
}

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
private fun <T : Any> JsonArray.toValue(klass: KClass<T>): T {
    val serializer = klass.serializer

    val jsonObject = JsonObject(
        mapOf(
            *this.mapIndexed { i, e ->
                serializer.descriptor.getElementName(i) to e
            }.toTypedArray()
        )
    )

    return json.decodeFromJsonElement(serializer, jsonObject)
}

@OptIn(InternalSerializationApi::class)
private val <T : Any> KClass<T>.serializer
    get() = companionObjectInstance?.takeIf { it.instanceOf(KSerializer::class) } as? KSerializer<T>
        ?: serializer()