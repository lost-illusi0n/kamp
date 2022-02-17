package net.lostillusion.kamp.client.rpc

import net.lostillusion.kamp.client.WampException
import net.lostillusion.kamp.client.WampMessage
import net.lostillusion.kamp.client.WampSession
import kotlin.jvm.JvmName

public class WampRemoteRpcException(
    public val procedure: Procedure<*, *, *, *>,
    public val error: WampMessage.Error
) : WampException("An error message was returned after calling the RPC, ${procedure.identifier}!\n$error")

public suspend fun <Input, InputKw, Output, OutputKw> WampSession.call(
    procedure: Procedure<Input, InputKw, Output, OutputKw>,
    input: Input? = null,
    inputKw: InputKw? = null
): Pair<Output?, OutputKw?> {
    val args = input?.let { procedure.convertToArgs(it) }
    val argsKw = inputKw?.let { procedure.convertToArgsKw(it) }

    val response = call(procedure.identifier, args, argsKw)

    if (response is WampMessage.Error) throw WampRemoteRpcException(procedure, response)

    response as WampMessage.Result

    val output = response.arguments?.let { procedure.convertToOutput(it) }
    val outputKw = response.argumentsKw?.let { procedure.convertToOutputKw(it) }

    return Pair(output, outputKw)
}

public suspend fun <Input, Output> WampSession.call(
    procedure: Procedure<Input, Nothing, Output, Nothing>,
    input: Input
): Output =
    call(procedure, input, null).first!!

@JvmName("callKw")
public suspend fun <InputKw, OutputKw> WampSession.call(
    procedure: Procedure<Nothing, InputKw, Nothing, OutputKw>,
    inputKw: InputKw
): OutputKw =
    call(procedure, null, inputKw).second!!