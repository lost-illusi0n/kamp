package net.lostillusion.kamp.client.rpc

import net.lostillusion.kamp.client.*

public class WampRemoteRpcException(
    public val procedure: Procedure,
    public val error: WampMessage.Error
) : WampException("An error message was returned after calling an RPC(${procedure.identifier})! Received the following message:\n$error")

internal suspend fun WampSession.call(
    procedure: Procedure,
    args: Arguments = emptyList(),
    argsKw: ArgumentsKw = emptyMap()
): Pair<Arguments?, ArgumentsKw?> {
    val response = call(procedure.identifier, args, argsKw)

    if (response is WampMessage.Error) throw WampRemoteRpcException(procedure, response)

    response as WampMessage.Result

    return Pair(response.arguments, response.argumentsKw)
}

public suspend fun WampSession.call(
    procedure: Procedure.Nothing
) {
    call(procedure, emptyList(), emptyMap())
}

public suspend fun <Input> WampSession.call(
    procedure: Procedure.OneWay.InputType.Input<Input>,
    input: Input
) {
    call(procedure, procedure.convertFromInput(input))
}

public suspend fun <InputKw> WampSession.call(
    procedure: Procedure.OneWay.InputType.InputKw<InputKw>,
    inputKw: InputKw
) {
    call(procedure, emptyList(), procedure.convertFromInput(inputKw))
}

public suspend fun <Input, InputKw> WampSession.call(
    procedure: Procedure.OneWay.InputType.Full<Input, InputKw>,
    input: Input,
    inputKw: InputKw
) {
    val (args, argsKw) = procedure.convertFromInput(input, inputKw)
    call(procedure, args, argsKw)
}

public suspend fun <Output> WampSession.call(
    procedure: Procedure.OneWay.OutputType.Output<Output>
): Output {
    val args = call(procedure, emptyList(), emptyMap()).first!!

    return procedure.convertToOutput(args)
}

public suspend fun <OutputKw> WampSession.call(
    procedure: Procedure.OneWay.OutputType.OutputKw<OutputKw>
): OutputKw {
    val argsKw = call(procedure, emptyList(), emptyMap()).second!!

    return procedure.convertToOutput(argsKw)
}

public suspend fun <Output, OutputKw> WampSession.call(
    procedure: Procedure.OneWay.OutputType.Full<Output, OutputKw>
): Pair<Output, OutputKw> {
    val (args, argsKw) = call(procedure, emptyList(), emptyMap())

    return procedure.convertToOutput(args!!, argsKw!!)
}

public suspend fun <Input, Output> WampSession.call(
    procedure: Procedure.Simple.Input.Output<Input, Output>,
    input: Input
): Output {
    val args = call(procedure, procedure.convertFromInput(input)).first!!

    return procedure.convertToOutput(args)
}

public suspend fun <Input, OutputKw> WampSession.call(
    procedure: Procedure.Simple.Input.OutputKw<Input, OutputKw>,
    input: Input
): OutputKw {
    val argsKw = call(procedure, procedure.convertFromInput(input)).second!!

    return procedure.convertToOutput(argsKw)
}

public suspend fun <InputKw, Output> WampSession.call(
    procedure: Procedure.Simple.InputKw.Output<InputKw, Output>,
    inputKw: InputKw
): Output {
    val args = call(procedure, emptyList(), procedure.convertFromInput(inputKw)).first!!

    return procedure.convertToOutput(args)
}

public suspend fun <InputKw, OutputKw> WampSession.call(
    procedure: Procedure.Simple.InputKw.OutputKw<InputKw, OutputKw>,
    inputKw: InputKw
): OutputKw {
    val argsKw = call(procedure, emptyList(), procedure.convertFromInput(inputKw)).second!!

    return procedure.convertToOutput(argsKw)
}

public suspend fun <Input, InputKw, Output> WampSession.call(
    procedure: Procedure.Complex.Input.Output<Input, InputKw, Output>,
    input: Input,
    inputKw: InputKw
): Output {
    val (args, argsKw) = procedure.convertFromInput(input, inputKw)
    val outputArgs = call(procedure, args, argsKw).first!!

    return procedure.convertToOutput(outputArgs)
}

public suspend fun <Input, InputKw, OutputKw> WampSession.call(
    procedure: Procedure.Complex.Input.OutputKw<Input, InputKw, OutputKw>,
    input: Input,
    inputKw: InputKw
): OutputKw {
    val (args, argsKw) = procedure.convertFromInput(input, inputKw)
    val outputArgsKw = call(procedure, args, argsKw).second!!

    return procedure.convertToOutput(outputArgsKw)
}

public suspend fun <Input, Output, OutputKw> WampSession.call(
    procedure: Procedure.Complex.Output.Input<Input, Output, OutputKw>,
    input: Input
): Pair<Output, OutputKw> {
    val inputArgs = procedure.convertFromInput(input)
    val (args, argsKw) = call(procedure, inputArgs, emptyMap())

    return procedure.convertToOutput(args!!, argsKw!!)
}

public suspend fun <InputKw, Output, OutputKw> WampSession.call(
    procedure: Procedure.Complex.Output.InputKw<InputKw, Output, OutputKw>,
    inputKw: InputKw
): Pair<Output, OutputKw> {
    val inputArgsKw = procedure.convertFromInput(inputKw)
    val (args, argsKw) = call(procedure, emptyList(), inputArgsKw)

    return procedure.convertToOutput(args!!, argsKw!!)
}

public suspend fun <Input, InputKw, Output, OutputKw> WampSession.call(
    procedure: Procedure.Full<Input, InputKw, Output, OutputKw>,
    input: Input,
    inputKw: InputKw
): Pair<Output, OutputKw> {
    val (iArgs, iArgsKw) = procedure.convertFromInput(input, inputKw)
    val (oArgs, oArgsKw) = call(procedure, iArgs, iArgsKw)

    return procedure.convertToOutput(oArgs!!, oArgsKw!!)
}