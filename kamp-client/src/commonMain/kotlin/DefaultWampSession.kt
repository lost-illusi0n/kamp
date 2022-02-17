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
    public val config: SessionConfig,
    coroutineContext: CoroutineContext
) : WampSession {
    public val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext + CoroutineName("wamp-session"))

    private var session: Id by Delegates.notNull()
    private var sessionScopeCount = atomic(1L)

    private val _incoming: MutableSharedFlow<WampMessage> = MutableSharedFlow()
    public override val incoming: SharedFlow<WampMessage> = _incoming

    init {
        socket.incoming
            .catch { error(it) }
            .onEach { _incoming.emit(it) }
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

    internal suspend fun hello() {
        send(config.intoHello())
        val welcome = socket.incoming.firstOf<WampMessage.Welcome>()
        session = welcome.session
    }

    override suspend fun goodbye(message: String) {
        socket.send(
            WampMessage.Goodbye(
                details = WampMessage.Goodbye.Details(message),
                reason = WampClose.SystemShutdown
            )
        )

        val remoteGoodbye = withTimeoutOrNull(config.messageTimeout) {
            incoming.firstOf<WampMessage.Goodbye>()
        } ?: return

        socket.close()
        shutdown()

        if (remoteGoodbye.reason != WampClose.GoodbyeAndOut) {
            logger.warn { "Expected a GoodbyeAndOut from peer but received ${remoteGoodbye.reason} instead!" }
        }
    }

    // rpc, pubsub, convenience methods

    private val Long.id: Id
        get() = Id.from(this)

    override suspend fun call(procedure: URI, args: Arguments, kwArgs: ArgumentsKw): WampCallResponse = coroutineScope {
        val requestId = sessionScopeCount.getAndIncrement().id

        val call = WampMessage.Call(
            requestId,
            WampMessage.Call.Options(),
            procedure,
            args,
            kwArgs
        )

        send(call)

        val response = withTimeoutOrNull(config.messageTimeout) {
            incoming
                .filterIsInstance<WampCallResponse>()
                .filter { it.request == requestId }
                .first()
        } ?: throw WampMessageTimeoutException(socket.host, config.messageTimeout)

        response
    }
}

public class WampMessageTimeoutException(host: NetworkAddress, timeout: Long) :
    WampSessionException(host, "Expected a message response within ${timeout}ms, but one was not received!")