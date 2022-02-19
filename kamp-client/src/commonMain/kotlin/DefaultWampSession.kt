package net.lostillusion.kamp.client

import io.ktor.util.network.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import net.lostillusion.kamp.client.transport.WampTransportSocket
import net.lostillusion.kamp.client.transport.firstOf
import net.lostillusion.kamp.client.transport.raw.WampTransportClosedException
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

private val logger = KotlinLogging.logger { }

public class DefaultWampSession(
    public val socket: WampTransportSocket,
    public val config: WampSessionConfig,
    coroutineContext: CoroutineContext
) : WampSession {
    public override val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext + CoroutineName("wamp-session"))

    private var session: Id by Delegates.notNull()
    private var sessionScopeCount = atomic(1L)

    private val _incoming: MutableSharedFlow<WampMessage> = MutableSharedFlow()
    public override val incoming: SharedFlow<WampMessage> = _incoming

    init {
        socket.incoming
            .catch { error(it) }
            .onEach { _incoming.emit(it) }
            .buffer(32)
            .launchIn(scope)
    }

    private suspend fun error(cause: Throwable? = null) {
        // todo: handle errors propagated

        if (cause is WampTransportClosedException) {
            logger.info(cause) { "WAMP Session was closed due to the following exception:" }
            shutdown()
            return
        }

        logger.error(cause) { "WAMP Session encountered the following exception:" }
    }

    private suspend fun awaitSubscriptionCompletion() {
        _incoming.subscriptionCount.takeWhile { it > 0 }.collect()
    }

    private suspend fun shutdown() {
        awaitSubscriptionCompletion()
        scope.cancel()
    }

    public override suspend fun send(message: WampMessage) {
        socket.send(message)
    }

    internal suspend fun hello() = coroutineScope {
        val welcomeDeferred = async { incoming.firstOf<WampMessage.Welcome>() }

        send(config.intoHello())

        val welcome = withTimeoutOrNull(config.messageTimeout) {
            welcomeDeferred.await()
        } ?: throw WampMessageTimeoutException(socket.host, config.messageTimeout)

        session = welcome.session
    }

    override suspend fun goodbye(message: String): Unit = coroutineScope {
        val remoteGoodbyeDeferred = async {
            incoming.firstOf<WampMessage.Goodbye>()
        }

        socket.send(
            WampMessage.Goodbye(
                details = WampMessage.Goodbye.Details(message),
                reason = WampClose.SystemShutdown
            )
        )

        val remoteGoodbye = withTimeoutOrNull(config.messageTimeout) {
            remoteGoodbyeDeferred.await()
        } ?: return@coroutineScope

        socket.close()
        shutdown()

        if (remoteGoodbye.reason != WampClose.GoodbyeAndOut) {
            logger.warn { "Expected a GoodbyeAndOut from peer but received ${remoteGoodbye.reason} instead!" }
        }
    }

    // rpc, pubsub, convenience methods

    private val Long.id: Id
        get() = Id.from(this)

    override suspend fun call(procedure: URI, args: Arguments?, argsKw: ArgumentsKw?): WampCallResponse = coroutineScope {
        val requestId = sessionScopeCount.getAndIncrement().id

        val call = WampMessage.Call(
            requestId,
            WampMessage.Call.Options(),
            procedure,
            args,
            argsKw
        )

        val responseDeferred = async {
            incoming
                .filterIsInstance<WampCallResponse>()
                .filter { it.request == requestId }
                .first()
        }

        send(call)

        val response = withTimeoutOrNull(config.messageTimeout) {
            responseDeferred.await()
        } ?: throw WampMessageTimeoutException(socket.host, config.messageTimeout)

        response
    }

    override suspend fun register(
        procedure: URI
    ): WampRegisterResponse = coroutineScope {
        val requestId = sessionScopeCount.getAndIncrement().id

        val register = WampMessage.Register(
            requestId,
            WampMessage.Register.Details(),
            procedure
        )

        val responseDeferred = async {
            incoming
                .filterIsInstance<WampRegisterResponse>()
                .filter { it.request == requestId }
                .first()
        }

        send(register)

        val response = withTimeoutOrNull(config.messageTimeout) {
            responseDeferred.await()
        } ?: throw WampMessageTimeoutException(socket.host, config.messageTimeout)

        response
    }

    override suspend fun unregister(registration: Id): WampUnregisterResponse = coroutineScope {
        val requestId = sessionScopeCount.getAndIncrement().id

        val unregister = WampMessage.Unregister(
            requestId,
            registration
        )

        val responseDeferred = async {
            incoming
                .filterIsInstance<WampUnregisterResponse>()
                .filter { it.request == requestId }
                .first()
        }

        send(unregister)

        val response = withTimeoutOrNull(config.messageTimeout) {
            responseDeferred.await()
        } ?: throw WampMessageTimeoutException(socket.host, config.messageTimeout)

        response
    }

    override suspend fun yield(requestId: Id, arguments: Arguments, argumentsKw: ArgumentsKw) {
        val yield = WampMessage.Yield(
            requestId,
            WampMessage.Yield.Options(),
            arguments,
            argumentsKw
        )

        send(`yield`)
    }
}

public class WampMessageTimeoutException(host: NetworkAddress, timeout: Long) :
    WampSessionException(host, "Expected a message response within ${timeout}ms, but one was not received!")