@file:OptIn(InternalSerializationApi::class)

package net.lostillusion.kamp.client.rpc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import net.lostillusion.kamp.client.Arguments
import net.lostillusion.kamp.client.ArgumentsKw
import net.lostillusion.kamp.client.URI

/**
 * NOTHING
 * Nothing -> Nothing
 *
 * ONE-WAY
 *  INPUT
 *   Input -> Nothing
 *   InputKw -> Nothing
 *   Input, InputKw -> Nothing
 *  OUTPUT
 *   Nothing -> Output
 *   Nothing -> OutputKw
 *   Nothing -> Output, OutputKw
 *
 * SIMPLE
 *  INPUT
 *   Input -> Output
 *   Input -> OutputKw
 *  INPUTKW
 *   InputKw -> Output
 *   InputKw -> OutputKw
 *
 * COMPLEX
 *  COMPLEX INPUT
 *   Input, InputKw -> Output
 *   Input, InputKw -> OutputKw
 *  COMPLEX OUTPUT
 *   Input -> Output, OutputKw
 *   InputKw -> Output, OutputKw
 *
 * FULL
 * Input, InputKw -> Output, OutputKw
 *
 * Every type should implement the following, if applicable:
 * - convertToInput(InputStructure as args(kw)) : InputStructure
 * - convertToOutput(OutputStructure as args(kw)) : OutputStructure
 * - convertFromInput(inputStructure) : InputStructure as args(kw)
 * - convertFromOutput(outputStructure) : OutputStructure as args(kw)
 */
public sealed class Procedure(public val identifier: URI) {
    public class Nothing(identifier: URI) : Procedure(identifier)

    public sealed class OneWay(identifier: URI) : Procedure(identifier) {
        public sealed class InputType(identifier: URI) : OneWay(identifier) {
            public class Input<Input>(identifier: URI, public val serializer: KSerializer<Input>) :
                InputType(identifier) {
                internal fun convertToInput(args: Arguments): Input = convertFromArgs(args, serializer)
                internal fun convertFromInput(input: Input): Arguments = convertToArgs(input, serializer)
            }

            public class InputKw<InputKw>(identifier: URI, public val serializer: KSerializer<InputKw>) :
                InputType(identifier) {
                internal fun convertToInput(argsKw: ArgumentsKw): InputKw = convertFromArgsKw(argsKw, serializer)
                internal fun convertFromInput(inputKw: InputKw): ArgumentsKw = convertToArgsKw(inputKw, serializer)
            }

            public class Full<Input, InputKw>(
                identifier: URI,
                public val inputSerializer: KSerializer<Input>,
                public val inputKwSerializer: KSerializer<InputKw>
            ) : InputType(identifier) {
                internal fun convertToInput(args: Arguments, argsKw: ArgumentsKw): Pair<Input, InputKw> {
                    val input = convertFromArgs(args, inputSerializer)
                    val inputKw = convertFromArgsKw(argsKw, inputKwSerializer)

                    return input to inputKw
                }

                internal fun convertFromInput(input: Input, inputKw: InputKw): Pair<Arguments, ArgumentsKw> {
                    val args = convertToArgs(input, inputSerializer)
                    val argsKw = convertToArgsKw(inputKw, inputKwSerializer)

                    return args to argsKw
                }
            }
        }

        public sealed class OutputType(identifier: URI) : OneWay(identifier) {
            public class Output<Output>(identifier: URI, public val serializer: KSerializer<Output>) :
                OutputType(identifier) {
                internal fun convertToOutput(args: Arguments): Output = convertFromArgs(args, serializer)
                internal fun convertFromOutput(value: Output): Arguments = convertToArgs(value, serializer)
            }

            public class OutputKw<OutputKw>(
                identifier: URI,
                public val serializer: KSerializer<OutputKw>
            ) : OutputType(identifier) {
                internal fun convertToOutput(argsKw: ArgumentsKw): OutputKw = convertFromArgsKw(argsKw, serializer)
                internal fun convertFromOutput(value: OutputKw): ArgumentsKw = convertToArgsKw(value, serializer)
            }

            public class Full<Output, OutputKw>(
                identifier: URI,
                public val outputSerializer: KSerializer<Output>,
                public val outputKwSerializer: KSerializer<OutputKw>
            ) : OutputType(identifier) {
                internal fun convertToOutput(args: Arguments, argsKw: ArgumentsKw): Pair<Output, OutputKw> {
                    val output = convertFromArgs(args, outputSerializer)
                    val outputKw = convertFromArgsKw(argsKw, outputKwSerializer)

                    return output to outputKw
                }

                internal fun convertFromOutput(output: Output, outputKw: OutputKw): Pair<Arguments, ArgumentsKw> {
                    val args = convertToArgs(output, outputSerializer)
                    val argsKw = convertToArgsKw(outputKw, outputKwSerializer)

                    return args to argsKw
                }
            }
        }
    }

    public sealed class Simple<InputType, OutputType>(
        identifier: URI,
        public val inputTypeSerializer: KSerializer<InputType>,
        public val outputTypeSerializer: KSerializer<OutputType>
    ) : Procedure(identifier) {
        public sealed class Input<Input, OutputType>(
            identifier: URI,
            inputSerializer: KSerializer<Input>,
            outputTypeSerializer: KSerializer<OutputType>
        ) : Simple<Input, OutputType>(identifier, inputSerializer, outputTypeSerializer) {
            public class Output<I, Output>(
                identifier: URI,
                inputSerializer: KSerializer<I>,
                outputSerializer: KSerializer<Output>
            ) : Input<I, Output>(identifier, inputSerializer, outputSerializer) {
                internal fun convertToOutput(args: Arguments): Output = convertFromArgs(args, outputTypeSerializer)
                internal fun convertFromOutput(value: Output): Arguments = convertToArgs(value, outputTypeSerializer)
            }

            public class OutputKw<I, OutputKw>(
                identifier: URI,
                inputSerializer: KSerializer<I>,
                outputKwSerializer: KSerializer<OutputKw>
            ) : Input<I, OutputKw>(identifier, inputSerializer, outputKwSerializer) {
                internal fun convertToOutput(argsKw: ArgumentsKw): OutputKw = convertFromArgsKw(argsKw, outputTypeSerializer)
                internal fun convertFromOutput(value: OutputKw): ArgumentsKw = convertToArgsKw(value, outputTypeSerializer)
            }

            internal fun convertToInput(args: Arguments): Input = convertFromArgs(args, inputTypeSerializer)
            internal fun convertFromInput(input: Input): Arguments = convertToArgs(input, inputTypeSerializer)
        }

        public sealed class InputKw<InputKw, OutputType>(
            identifier: URI,
            inputKwSerializer: KSerializer<InputKw>,
            outputTypeSerializer: KSerializer<OutputType>
        ) : Simple<InputKw, OutputType>(identifier, inputKwSerializer, outputTypeSerializer) {
            public class Output<IKw, Output>(
                identifier: URI,
                inputKwSerializer: KSerializer<IKw>,
                outputSerializer: KSerializer<Output>
            ) : InputKw<IKw, Output>(identifier, inputKwSerializer, outputSerializer) {
                internal fun convertToOutput(args: Arguments): Output = convertFromArgs(args, outputTypeSerializer)
                internal fun convertFromOutput(value: Output): Arguments = convertToArgs(value, outputTypeSerializer)
            }

            public class OutputKw<IKw, OutputKw>(
                identifier: URI,
                inputKwSerializer: KSerializer<IKw>,
                outputKwSerializer: KSerializer<OutputKw>
            ) : InputKw<IKw, OutputKw>(identifier, inputKwSerializer, outputKwSerializer) {
                internal fun convertToOutput(argsKw: ArgumentsKw): OutputKw =
                    convertFromArgsKw(argsKw, outputTypeSerializer)

                internal fun convertFromOutput(value: OutputKw): ArgumentsKw =
                    convertToArgsKw(value, outputTypeSerializer)
            }

            internal fun convertToInput(argsKw: ArgumentsKw): InputKw = convertFromArgsKw(argsKw, inputTypeSerializer)
            internal fun convertFromInput(value: InputKw): ArgumentsKw = convertToArgsKw(value, inputTypeSerializer)
        }
    }

    public sealed class Complex(
        identifier: URI,
    ) : Procedure(identifier) {
        public sealed class Input<Input, InputKw, OutputType>(
            identifier: URI,
            public val inputSerializer: KSerializer<Input>,
            public val inputKwSerializer: KSerializer<InputKw>,
            public val outputTypeSerializer: KSerializer<OutputType>
        ) : Complex(identifier) {
            public class Output<I, InputKw, Output>(
                identifier: URI,
                inputSerializer: KSerializer<I>,
                inputKwSerializer: KSerializer<InputKw>,
                outputSerializer: KSerializer<Output>
            ) : Input<I, InputKw, Output>(
                identifier,
                inputSerializer,
                inputKwSerializer,
                outputSerializer
            ) {
                internal fun convertToOutput(args: Arguments): Output = convertFromArgs(args, outputTypeSerializer)
                internal fun convertFromOutput(value: Output): Arguments = convertToArgs(value, outputTypeSerializer)
            }

            public class OutputKw<I, InputKw, OutputKw>(
                identifier: URI,
                inputSerializer: KSerializer<I>,
                inputKwSerializer: KSerializer<InputKw>,
                outputKwSerializer: KSerializer<OutputKw>
            ) : Input<I, InputKw, OutputKw>(
                identifier,
                inputSerializer,
                inputKwSerializer,
                outputKwSerializer
            ) {
                internal fun convertToOutput(argsKw: ArgumentsKw): OutputKw =
                    convertFromArgsKw(argsKw, outputTypeSerializer)

                internal fun convertFromOutput(value: OutputKw): ArgumentsKw =
                    convertToArgsKw(value, outputTypeSerializer)
            }

            internal fun convertToInput(args: Arguments, argsKw: ArgumentsKw): Pair<Input, InputKw> {
                val input = convertFromArgs(args, inputSerializer)
                val inputKw = convertFromArgsKw(argsKw, inputKwSerializer)

                return input to inputKw
            }

            internal fun convertFromInput(input: Input, inputKw: InputKw): Pair<Arguments, ArgumentsKw> {
                val args = convertToArgs(input, inputSerializer)
                val argsKw = convertToArgsKw(inputKw, inputKwSerializer)

                return args to argsKw
            }
        }

        public sealed class Output<InputType, Output, OutputKw>(
            identifier: URI,
            public val inputTypeSerializer: KSerializer<InputType>,
            public val outputSerializer: KSerializer<Output>,
            public val outputKwSerializer: KSerializer<OutputKw>
        ) : Complex(identifier) {
            public class Input<Input, O, OutputKw>(
                identifier: URI,
                inputSerializer: KSerializer<Input>,
                outputSerializer: KSerializer<O>,
                outputKwSerializer: KSerializer<OutputKw>
            ) : Output<Input, O, OutputKw>(
                identifier,
                inputSerializer,
                outputSerializer,
                outputKwSerializer
            ) {
                internal fun convertToInput(args: Arguments): Input = convertFromArgs(args, inputTypeSerializer)
                internal fun convertFromInput(value: Input): Arguments = convertToArgs(value, inputTypeSerializer)
            }

            public class InputKw<InputKw, O, OutputKw>(
                identifier: URI,
                inputKwSerializer: KSerializer<InputKw>,
                outputSerializer: KSerializer<O>,
                outputKwSerializer: KSerializer<OutputKw>
            ) : Output<InputKw, O, OutputKw>(
                identifier,
                inputKwSerializer,
                outputSerializer,
                outputKwSerializer
            ) {
                internal fun convertToInput(argsKw: ArgumentsKw): InputKw =
                    convertFromArgsKw(argsKw, inputTypeSerializer)

                internal fun convertFromInput(value: InputKw): ArgumentsKw = convertToArgsKw(value, inputTypeSerializer)
            }

            internal fun convertToOutput(args: Arguments, argsKw: ArgumentsKw): Pair<Output, OutputKw> {
                val output = convertFromArgs(args, outputSerializer)
                val outputKw = convertFromArgsKw(argsKw, outputKwSerializer)

                return output to outputKw
            }

            internal fun convertFromOutput(output: Output, outputKw: OutputKw): Pair<Arguments, ArgumentsKw> {
                val args = convertToArgs(output, outputSerializer)
                val argsKw = convertToArgsKw(outputKw, outputKwSerializer)

                return args to argsKw
            }
        }
    }

    public class Full<Input, InputKw, Output, OutputKw>(
        identifier: URI,
        public val inputSerializer: KSerializer<Input>,
        public val inputKwSerializer: KSerializer<InputKw>,
        public val outputSerializer: KSerializer<Output>,
        public val outputKwSerializer: KSerializer<OutputKw>
    ) : Procedure(identifier) {
        internal fun convertToInput(args: Arguments, argsKw: ArgumentsKw): Pair<Input, InputKw> {
            val input = convertFromArgs(args, inputSerializer)
            val inputKw = convertFromArgsKw(argsKw, inputKwSerializer)

            return input to inputKw
        }

        internal fun convertFromInput(input: Input, inputKw: InputKw): Pair<Arguments, ArgumentsKw> {
            val args = convertToArgs(input, inputSerializer)
            val argsKw = convertToArgsKw(inputKw, inputKwSerializer)

            return args to argsKw
        }

        internal fun convertToOutput(args: Arguments, argsKw: ArgumentsKw): Pair<Output, OutputKw> {
            val output = convertFromArgs(args, outputSerializer)
            val outputKw = convertFromArgsKw(argsKw, outputKwSerializer)

            return output to outputKw
        }

        internal fun convertFromOutput(output: Output, outputKw: OutputKw): Pair<Arguments, ArgumentsKw> {
            val args = convertToArgs(output, outputSerializer)
            val argsKw = convertToArgsKw(outputKw, outputKwSerializer)

            return args to argsKw
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun <T> convertFromArgs(args: Arguments, serializer: KSerializer<T>): T {
        return when (serializer.descriptor.kind) {
            is PrimitiveKind -> {
                require(args.size == 1)

                Json.decodeFromJsonElement(serializer, args.first())
            }
            StructureKind.LIST -> {
                Json.decodeFromJsonElement(serializer, JsonArray(args))
            }
            is StructureKind -> {
                // names were stripped, get them back
                val obj = JsonObject(
                    args.mapIndexed { index, jsonElement ->
                        serializer.descriptor.getElementName(index) to jsonElement
                    }.associate { it }
                )

                Json.decodeFromJsonElement(serializer, obj)
            }
            else -> error("not supported")
        }
    }

    internal fun <T> convertToArgs(value: T, serializer: KSerializer<T>): Arguments {
        return when (val base = Json.encodeToJsonElement(serializer, value)) {
            is JsonPrimitive -> listOf(base)
            is JsonObject -> base.values.toList()
            is JsonArray -> base
            JsonNull -> emptyList()
        }
    }

    internal fun <T> convertFromArgsKw(argsKw: ArgumentsKw, serializer: KSerializer<T>): T {
        return Json.decodeFromJsonElement(serializer, JsonObject(argsKw))
    }

    internal fun <T> convertToArgsKw(value: T, serializer: KSerializer<T>): ArgumentsKw {
        val base = Json.encodeToJsonElement(serializer, value)

        require(base is JsonObject) { "value must serialize as a JsonObject!" }

        return base
    }
}

public fun procedure(identifier: URI): Procedure.Nothing = Procedure.Nothing(identifier)

public inline fun <reified Input : Any> inputProcedure(
    identifier: URI,
    inputSerializer: KSerializer<Input> = Input::class.serializer()
): Procedure.OneWay.InputType.Input<Input> =
    Procedure.OneWay.InputType.Input(identifier, inputSerializer)

public inline fun <reified InputKw : Any> inputKwProcedure(
    identifier: URI,
    inputKwSerializer: KSerializer<InputKw> = InputKw::class.serializer()
): Procedure.OneWay.InputType.InputKw<InputKw> =
    Procedure.OneWay.InputType.InputKw(identifier, inputKwSerializer)

public inline fun <reified Input : Any, reified InputKw : Any> inputFullProcedure(
    identifier: URI,
    inputSerializer: KSerializer<Input> = Input::class.serializer(),
    inputKwSerializer: KSerializer<InputKw> = InputKw::class.serializer()
): Procedure.OneWay.InputType.Full<Input, InputKw> =
    Procedure.OneWay.InputType.Full(
        identifier,
        inputSerializer,
        inputKwSerializer
    )

public inline fun <reified Output : Any> outputProcedure(
    identifier: URI,
    outputSerializer: KSerializer<Output> = Output::class.serializer()
): Procedure.OneWay.OutputType.Output<Output> =
    Procedure.OneWay.OutputType.Output(identifier, outputSerializer)

public inline fun <reified OutputKw : Any> outputKwProcedure(
    identifier: URI,
    outputKwSerializer: KSerializer<OutputKw> = OutputKw::class.serializer()
): Procedure.OneWay.OutputType.OutputKw<OutputKw> =
    Procedure.OneWay.OutputType.OutputKw(identifier, outputKwSerializer)

public inline fun <reified Output : Any, reified OutputKw : Any> outputFullProcedure(
    identifier: URI,
    outputSerializer: KSerializer<Output> = Output::class.serializer(),
    outputKwSerializer: KSerializer<OutputKw> = OutputKw::class.serializer()
): Procedure.OneWay.OutputType.Full<Output, OutputKw> =
    Procedure.OneWay.OutputType.Full(identifier, outputSerializer, outputKwSerializer)

public inline fun <reified Input : Any, reified Output : Any> simpleProcedure(
    identifier: URI,
    inputSerializer: KSerializer<Input> = Input::class.serializer(),
    outputSerializer: KSerializer<Output> = Output::class.serializer()
): Procedure.Simple.Input.Output<Input, Output> =
    Procedure.Simple.Input.Output(identifier, inputSerializer, outputSerializer)

public inline fun <reified Input : Any, reified OutputKw : Any> simpleInputOutputKwProcedure(
    identifier: URI,
    inputSerializer: KSerializer<Input> = Input::class.serializer(),
    outputKwSerializer: KSerializer<OutputKw> = OutputKw::class.serializer()
): Procedure.Simple.Input.OutputKw<Input, OutputKw> =
    Procedure.Simple.Input.OutputKw(identifier, inputSerializer, outputKwSerializer)

public inline fun <reified InputKw : Any, reified OutputKw : Any> simpleKwProcedure(
    identifier: URI,
    inputKwSerializer: KSerializer<InputKw> = InputKw::class.serializer(),
    outputKwSerializer: KSerializer<OutputKw> = OutputKw::class.serializer()
): Procedure.Simple.InputKw.OutputKw<InputKw, OutputKw> =
    Procedure.Simple.InputKw.OutputKw(identifier, inputKwSerializer, outputKwSerializer)

public inline fun <reified InputKw : Any, reified Output : Any> simpleInputKwOutputProcedure(
    identifier: URI,
    inputKwSerializer: KSerializer<InputKw> = InputKw::class.serializer(),
    outputSerializer: KSerializer<Output> = Output::class.serializer()
): Procedure.Simple.InputKw.Output<InputKw, Output> =
    Procedure.Simple.InputKw.Output(identifier, inputKwSerializer, outputSerializer)

public inline fun <reified Input : Any, reified InputKw : Any, reified Output : Any> complexInputOutputProcedure(
    identifier: URI,
    inputSerializer: KSerializer<Input> = Input::class.serializer(),
    inputKwSerializer: KSerializer<InputKw> = InputKw::class.serializer(),
    outputSerializer: KSerializer<Output> = Output::class.serializer()
): Procedure.Complex.Input.Output<Input, InputKw, Output> =
    Procedure.Complex.Input.Output(identifier, inputSerializer, inputKwSerializer, outputSerializer)

public inline fun <reified Input : Any, reified InputKw : Any, reified OutputKw : Any> complexInputOutputKwProcedure(
    identifier: URI,
    inputSerializer: KSerializer<Input> = Input::class.serializer(),
    inputKwSerializer: KSerializer<InputKw> = InputKw::class.serializer(),
    outputSerializer: KSerializer<OutputKw> = OutputKw::class.serializer()
): Procedure.Complex.Input.OutputKw<Input, InputKw, OutputKw> =
    Procedure.Complex.Input.OutputKw(identifier, inputSerializer, inputKwSerializer, outputSerializer)

public inline fun <reified Input : Any, reified Output : Any, reified OutputKw : Any> complexOutputInputProcedure(
    identifier: URI,
    inputSerializer: KSerializer<Input> = Input::class.serializer(),
    outputSerializer: KSerializer<Output> = Output::class.serializer(),
    outputKwSerializer: KSerializer<OutputKw> = OutputKw::class.serializer()
): Procedure.Complex.Output.Input<Input, Output, OutputKw> =
    Procedure.Complex.Output.Input(identifier, inputSerializer, outputSerializer, outputKwSerializer)

public inline fun <reified InputKw : Any, reified Output : Any, reified OutputKw : Any> complexOutputInputKwProcedure(
    identifier: URI,
    inputKwSerializer: KSerializer<InputKw> = InputKw::class.serializer(),
    outputSerializer: KSerializer<Output> = Output::class.serializer(),
    outputKwSerializer: KSerializer<OutputKw> = OutputKw::class.serializer()
): Procedure.Complex.Output.InputKw<InputKw, Output, OutputKw> =
    Procedure.Complex.Output.InputKw(identifier, inputKwSerializer, outputSerializer, outputKwSerializer)

public inline fun <reified Input : Any, reified InputKw : Any, reified Output : Any, reified OutputKw : Any> fullProcedure(
    identifier: URI,
    inputSerializer: KSerializer<Input> = Input::class.serializer(),
    inputKwSerializer: KSerializer<InputKw> = InputKw::class.serializer(),
    outputSerializer: KSerializer<Output> = Output::class.serializer(),
    outputKwSerializer: KSerializer<OutputKw> = OutputKw::class.serializer()
): Procedure.Full<Input, InputKw, Output, OutputKw> =
    Procedure.Full(identifier, inputSerializer, inputKwSerializer, outputSerializer, outputKwSerializer)