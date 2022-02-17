package net.lostillusion.kamp.client.rpc

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.*
import net.lostillusion.kamp.client.Arguments
import net.lostillusion.kamp.client.ArgumentsKw
import net.lostillusion.kamp.client.URI

public data class Procedure<Input, InputKw, Output, OutputKw>(
    val identifier: URI,
    val inputSerializer: SerializationStrategy<Input>? = null,
    val inputKwSerializer: SerializationStrategy<InputKw>? = null,
    val outputSerializer: DeserializationStrategy<Output>? = null,
    val outputKwSerializer: DeserializationStrategy<OutputKw>? = null,
    val json: Json = Json
) {
    internal fun convertToArgs(args: Input): Arguments {
        require(inputSerializer != null)

        return when (val base = json.encodeToJsonElement(inputSerializer, args)) {
            is JsonPrimitive -> listOf(base)
            is JsonObject -> base.values.toList()
            is JsonArray -> base
            JsonNull -> emptyList()
        }
    }

    internal fun convertToArgsKw(argsKw: InputKw): ArgumentsKw {
        require(inputKwSerializer != null)

        val base = json.encodeToJsonElement(inputKwSerializer, argsKw)

        require(base is JsonObject) { "ArgsKw must be a JsonObject!" }

        return base
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun convertToOutput(args: Arguments): Output {
        require(outputSerializer != null)

        return when (outputSerializer.descriptor.kind) {
            is PrimitiveKind -> {
                require(args.size == 1)

                json.decodeFromJsonElement(outputSerializer, args.first())
            }
            StructureKind.LIST -> {
                json.decodeFromJsonElement(outputSerializer, JsonArray(args))
            }
            is StructureKind -> {
                // names were stripped, get them back
                val obj = JsonObject(
                    args
                        .mapIndexed { index, jsonElement ->
                            outputSerializer.descriptor.getElementName(index) to jsonElement
                        }.associate { it }
                )

                json.decodeFromJsonElement(outputSerializer, obj)
            }
            else -> error("not supported")
        }
    }

    internal fun convertToOutputKw(argsKw: ArgumentsKw): OutputKw {
        require(outputKwSerializer != null)

        return json.decodeFromJsonElement(outputKwSerializer, JsonObject(argsKw))
    }
}

@OptIn(InternalSerializationApi::class)
public inline fun <reified Input : Any, reified Output : Any> procedure(
    identifier: URI,
    inputSerializer: SerializationStrategy<Input> = Input::class.serializer(),
    outputSerializer: DeserializationStrategy<Output> = Output::class.serializer()
): Procedure<Input, Nothing, Output, Nothing> = Procedure(identifier, inputSerializer, null, outputSerializer, null)

@OptIn(InternalSerializationApi::class)
public inline fun <reified InputKw : Any, reified OutputKw : Any> procedureKw(
    identifier: URI,
    inputKwSerializer: SerializationStrategy<InputKw> = InputKw::class.serializer(),
    outputKwSerializer: DeserializationStrategy<OutputKw> = OutputKw::class.serializer()
): Procedure<Nothing, InputKw, Nothing, OutputKw> =
    Procedure(identifier, null, inputKwSerializer, null, outputKwSerializer)
