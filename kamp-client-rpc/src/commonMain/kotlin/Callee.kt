package net.lostillusion.kamp.client.rpc

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonPrimitive
import net.lostillusion.kamp.client.*

public class WampInvocationErrorBuilder(public val request: Id) {
    public var reason: WampError = WampError.from(URI.unsafe("wamp.rpc.exception"))

    public var argumentsExceptions: MutableMap<Int, String> = mutableMapOf()
    public var argumentsKwExceptions: MutableMap<String, String> = mutableMapOf()

    public fun argument(reason: String, position: Int = argumentsExceptions.size + 1) {
        argumentsExceptions[position] = reason
    }

    public fun argument(name: String, reason: String) {
        argumentsKwExceptions[name] = reason
    }

    public fun build(): WampMessage.InvocationError = WampMessage.InvocationError(
        request,
        WampMessage.InvocationError.Details(),
        reason,
        argumentsExceptions.entries.sortedBy { it.key }.map { JsonPrimitive(it.value) }.takeIf { it.isNotEmpty() },
        argumentsKwExceptions.mapValues { JsonPrimitive(it.value) }.takeIf { it.isNotEmpty() }
    )
}

public sealed class WampRpcExecutionResult {
    public sealed class Nothing: WampRpcExecutionResult() {
        public object Success: Nothing()
        public class Error(public val error: WampMessage.InvocationError) : Nothing()
    }

    public sealed class Simple<Output>: WampRpcExecutionResult() {
        public class Success<Output>(public val output: Output) : Simple<Output>()
        public class Error(public val error: WampMessage.InvocationError) : Simple<Nothing>()
    }

    public sealed class Full<Output, OutputKw>: WampRpcExecutionResult() {
        public class Success<Output, OutputKw>(public val output: Output, public val outputKw: OutputKw) : Full<Output, OutputKw>()
        public class Error(public val error: WampMessage.InvocationError) : Full<Nothing, Nothing>()
    }
}

public sealed class WampRpcExecutionScope(public val request: Id) {
    public class Nothing(request: Id) : WampRpcExecutionScope(request) {
        public fun yield(): WampRpcExecutionResult.Nothing.Success = WampRpcExecutionResult.Nothing.Success

        public fun error(errorBuilder: WampInvocationErrorBuilder.() -> Unit): WampRpcExecutionResult.Nothing.Error {
            val error = WampInvocationErrorBuilder(request).apply(errorBuilder).build()

            return WampRpcExecutionResult.Nothing.Error(error)
        }
    }

    public class Simple<Output>(request: Id) : WampRpcExecutionScope(request) {
        public fun yield(value: Output): WampRpcExecutionResult.Simple.Success<Output> = WampRpcExecutionResult.Simple.Success(value)

        public fun error(errorBuilder: WampInvocationErrorBuilder.() -> Unit): WampRpcExecutionResult.Simple.Error {
            val error = WampInvocationErrorBuilder(request).apply(errorBuilder).build()

            return WampRpcExecutionResult.Simple.Error(error)
        }
    }

    public class Full<Output, OutputKw>(request: Id) : WampRpcExecutionScope(request) {
        public fun yield(output: Output, outputKw: OutputKw): WampRpcExecutionResult.Full.Success<Output, OutputKw> = WampRpcExecutionResult.Full.Success(output, outputKw)

        public fun error(errorBuilder: WampInvocationErrorBuilder.() -> Unit): WampRpcExecutionResult.Full.Error {
            val error = WampInvocationErrorBuilder(request).apply(errorBuilder).build()

            return WampRpcExecutionResult.Full.Error(error)
        }
    }
}

public data class ProcedureRegistration(
    val procedureExecutorJob: Job,
    val registration: Id
)

private data class RawWampExecutionResponse(
    val error: WampMessage.InvocationError? = null,
    val args: Arguments? = null,
    val argsKw: ArgumentsKw? = null
)

private suspend fun WampSession.register(
    procedure: Procedure,
    executionScope: (request: Id, Arguments?, ArgumentsKw?) -> RawWampExecutionResponse
): ProcedureRegistration {
    val registerResponse = register(procedure.identifier)

    if (registerResponse is WampMessage.RegisterError) TODO()

    require(registerResponse is WampMessage.Registered)

    val job = incoming.filterIsInstance<WampMessage.Invocation>()
        .filter { it.registration == registerResponse.registration }
        .map { it.request to executionScope.invoke(it.request, it.arguments, it.argumentsKw) }
        .onEach { (request, response) ->
            if (response.error != null) {
                return@onEach send(response.error)
            }

            yield(request, response.args.orEmpty(), response.argsKw.orEmpty())
        }
        .launchIn(scope)

    return ProcedureRegistration(job, registerResponse.registration)
}

public suspend fun WampSession.register(
    procedure: Procedure.Nothing,
    executionScope: WampRpcExecutionScope.Nothing.() -> WampRpcExecutionResult.Nothing
): ProcedureRegistration = register(procedure) { request, _, _ ->
    when (val response = executionScope.invoke(WampRpcExecutionScope.Nothing(request))) {
        WampRpcExecutionResult.Nothing.Success -> RawWampExecutionResponse()
        is WampRpcExecutionResult.Nothing.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Input> WampSession.register(
    procedure: Procedure.OneWay.InputType.Input<Input>,
    executionScope: WampRpcExecutionScope.Nothing.(Input) -> WampRpcExecutionResult.Nothing
): ProcedureRegistration = register(procedure) { request, args, _ ->
    val input = procedure.convertToInput(args!!)
    when (val response = executionScope.invoke(WampRpcExecutionScope.Nothing(request), input)) {
        WampRpcExecutionResult.Nothing.Success -> RawWampExecutionResponse()
        is WampRpcExecutionResult.Nothing.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <InputKw> WampSession.register(
    procedure: Procedure.OneWay.InputType.InputKw<InputKw>,
    executionScope: WampRpcExecutionScope.Nothing.(InputKw) -> WampRpcExecutionResult.Nothing
): ProcedureRegistration = register(procedure) { request, _, argsKw ->
    val input = procedure.convertToInput(argsKw!!)
    when (val response = executionScope.invoke(WampRpcExecutionScope.Nothing(request), input)) {
        WampRpcExecutionResult.Nothing.Success -> RawWampExecutionResponse()
        is WampRpcExecutionResult.Nothing.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Input, InputKw> WampSession.register(
    procedure: Procedure.OneWay.InputType.Full<Input, InputKw>,
    executionScope: WampRpcExecutionScope.Nothing.(Input, InputKw) -> WampRpcExecutionResult.Nothing
): ProcedureRegistration = register(procedure) { request, args, argsKw ->
    val (input, inputKw) = procedure.convertToInput(args!!, argsKw!!)
    when (val response = executionScope.invoke(WampRpcExecutionScope.Nothing(request), input, inputKw)) {
        WampRpcExecutionResult.Nothing.Success -> RawWampExecutionResponse()
        is WampRpcExecutionResult.Nothing.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Output> WampSession.register(
    procedure: Procedure.OneWay.OutputType.Output<Output>,
    executionScope: WampRpcExecutionScope.Simple<Output>.() -> WampRpcExecutionResult.Simple<Output>
): ProcedureRegistration = register(procedure) { request, _, _ ->
    when (val response = executionScope.invoke(WampRpcExecutionScope.Simple(request))) {
        is WampRpcExecutionResult.Simple.Success -> RawWampExecutionResponse(args = procedure.convertFromOutput(response.output))
        is WampRpcExecutionResult.Simple.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <OutputKw> WampSession.register(
    procedure: Procedure.OneWay.OutputType.OutputKw<OutputKw>,
    executionScope: WampRpcExecutionScope.Simple<OutputKw>.() -> WampRpcExecutionResult.Simple<OutputKw>
): ProcedureRegistration = register(procedure) { request, _, _ ->
    when (val response = executionScope.invoke(WampRpcExecutionScope.Simple(request))) {
        is WampRpcExecutionResult.Simple.Success -> RawWampExecutionResponse(argsKw = procedure.convertFromOutput(response.output))
        is WampRpcExecutionResult.Simple.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Output, OutputKw> WampSession.register(
    procedure: Procedure.OneWay.OutputType.Full<Output, OutputKw>,
    executionScope: WampRpcExecutionScope.Full<Output, OutputKw>.() -> WampRpcExecutionResult.Full<Output, OutputKw>
): ProcedureRegistration = register(procedure) { request, _, _ ->
    when (val response = executionScope.invoke(WampRpcExecutionScope.Full(request))) {
        is WampRpcExecutionResult.Full.Success -> {
            val (output, outputKw) = procedure.convertFromOutput(response.output, response.outputKw)

            RawWampExecutionResponse(null, output, outputKw)
        }
        is WampRpcExecutionResult.Full.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Input, Output> WampSession.register(
    procedure: Procedure.Simple.Input.Output<Input, Output>,
    executionScope: WampRpcExecutionScope.Simple<Output>.(Input) -> WampRpcExecutionResult.Simple<Output>
): ProcedureRegistration = register(procedure) { request, args, _ ->
    val input = procedure.convertToInput(args!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Simple(request), input)) {
        is WampRpcExecutionResult.Simple.Success -> RawWampExecutionResponse(args = procedure.convertFromOutput(response.output))
        is WampRpcExecutionResult.Simple.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Input, OutputKw> WampSession.register(
    procedure: Procedure.Simple.Input.OutputKw<Input, OutputKw>,
    executionScope: WampRpcExecutionScope.Simple<OutputKw>.(Input) -> WampRpcExecutionResult.Simple<OutputKw>
): ProcedureRegistration = register(procedure) { request, args, _ ->
    val input = procedure.convertToInput(args!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Simple(request), input)) {
        is WampRpcExecutionResult.Simple.Success -> RawWampExecutionResponse(argsKw = procedure.convertFromOutput(response.output))
        is WampRpcExecutionResult.Simple.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <InputKw, Output> WampSession.register(
    procedure: Procedure.Simple.InputKw.Output<InputKw, Output>,
    executionScope: WampRpcExecutionScope.Simple<Output>.(InputKw) -> WampRpcExecutionResult.Simple<Output>
): ProcedureRegistration = register(procedure) { request, _ , argsKw ->
    val input = procedure.convertToInput(argsKw!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Simple(request), input)) {
        is WampRpcExecutionResult.Simple.Success -> RawWampExecutionResponse(args = procedure.convertFromOutput(response.output))
        is WampRpcExecutionResult.Simple.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <InputKw, OutputKw> WampSession.register(
    procedure: Procedure.Simple.InputKw.OutputKw<InputKw, OutputKw>,
    executionScope: WampRpcExecutionScope.Simple<OutputKw>.(InputKw) -> WampRpcExecutionResult.Simple<OutputKw>
): ProcedureRegistration = register(procedure) { request, _ , argsKw ->
    val input = procedure.convertToInput(argsKw!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Simple(request), input)) {
        is WampRpcExecutionResult.Simple.Success -> RawWampExecutionResponse(argsKw = procedure.convertFromOutput(response.output))
        is WampRpcExecutionResult.Simple.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Input, InputKw, Output> WampSession.register(
    procedure: Procedure.Complex.Input.Output<Input, InputKw, Output>,
    executionScope: WampRpcExecutionScope.Simple<Output>.(Input, InputKw) -> WampRpcExecutionResult.Simple<Output>
): ProcedureRegistration = register(procedure) { request, args , argsKw ->
    val (input, inputKw) = procedure.convertToInput(args!!, argsKw!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Simple(request), input, inputKw)) {
        is WampRpcExecutionResult.Simple.Success -> RawWampExecutionResponse(args = procedure.convertFromOutput(response.output))
        is WampRpcExecutionResult.Simple.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Input, InputKw, OutputKw> WampSession.register(
    procedure: Procedure.Complex.Input.OutputKw<Input, InputKw, OutputKw>,
    executionScope: WampRpcExecutionScope.Simple<OutputKw>.(Input, InputKw) -> WampRpcExecutionResult.Simple<OutputKw>
): ProcedureRegistration = register(procedure) { request, args , argsKw ->
    val (input, inputKw) = procedure.convertToInput(args!!, argsKw!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Simple(request), input, inputKw)) {
        is WampRpcExecutionResult.Simple.Success -> RawWampExecutionResponse(argsKw = procedure.convertFromOutput(response.output))
        is WampRpcExecutionResult.Simple.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Input, Output, OutputKw> WampSession.register(
    procedure: Procedure.Complex.Output.Input<Input, Output, OutputKw>,
    executionScope: WampRpcExecutionScope.Full<Output, OutputKw>.(Input) -> WampRpcExecutionResult.Full<Output, OutputKw>
): ProcedureRegistration = register(procedure) { request, args , _ ->
    val input = procedure.convertToInput(args!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Full(request), input)) {
        is WampRpcExecutionResult.Full.Success -> {
            val (args, argsKw) = procedure.convertFromOutput(response.output, response.outputKw)
            RawWampExecutionResponse(null, args , argsKw)
        }
        is WampRpcExecutionResult.Full.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <InputKw, Output, OutputKw> WampSession.register(
    procedure: Procedure.Complex.Output.InputKw<InputKw, Output, OutputKw>,
    executionScope: WampRpcExecutionScope.Full<Output, OutputKw>.(InputKw) -> WampRpcExecutionResult.Full<Output, OutputKw>
): ProcedureRegistration = register(procedure) { request, _ , argsKw ->
    val input = procedure.convertToInput(argsKw!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Full(request), input)) {
        is WampRpcExecutionResult.Full.Success -> {
            val (args, argsKw) = procedure.convertFromOutput(response.output, response.outputKw)
            RawWampExecutionResponse(null, args , argsKw)
        }
        is WampRpcExecutionResult.Full.Error -> RawWampExecutionResponse(response.error)
    }
}

public suspend fun <Input, InputKw, Output, OutputKw> WampSession.register(
    procedure: Procedure.Full<Input, InputKw, Output, OutputKw>,
    executionScope: WampRpcExecutionScope.Full<Output, OutputKw>.(Input, InputKw) -> WampRpcExecutionResult.Full<Output, OutputKw>
): ProcedureRegistration = register(procedure) { request, args , argsKw ->
    val (input, inputKw) = procedure.convertToInput(args!!, argsKw!!)

    when (val response = executionScope.invoke(WampRpcExecutionScope.Full(request), input, inputKw)) {
        is WampRpcExecutionResult.Full.Success -> {
            val (args, argsKw) = procedure.convertFromOutput(response.output, response.outputKw)
            RawWampExecutionResponse(null, args , argsKw)
        }
        is WampRpcExecutionResult.Full.Error -> RawWampExecutionResponse(response.error)
    }
}