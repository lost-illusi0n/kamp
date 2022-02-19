package net.lostillusion.kamp.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

public interface WampSession {
    public val scope: CoroutineScope

    public val incoming: SharedFlow<WampMessage>

    public suspend fun send(message: WampMessage)

    public suspend fun goodbye(message: String)

    public suspend fun call(
        procedure: URI,
        args: Arguments? = null,
        argsKw: ArgumentsKw? = null,
    ): WampCallResponse

    public suspend fun register(
        procedure: URI,
    ): WampRegisterResponse

    public suspend fun unregister(
        registration: Id
    ): WampUnregisterResponse

    public suspend fun yield(requestId: Id)

    public suspend fun yield(requestId: Id, arguments: Arguments)

    public suspend fun yield(requestId: Id, argumentsKw: ArgumentsKw)

    public suspend fun yield(requestId: Id, arguments: Arguments, argumentsKw: ArgumentsKw)

    public suspend fun publishWithAck(topic: URI): WampPublishResponse

    public suspend fun publishWithAck(topic: URI, arguments: Arguments): WampPublishResponse

    public suspend fun publishWithAck(topic: URI, argumentsKw: ArgumentsKw): WampPublishResponse = publishWithAck(topic, emptyList(), argumentsKw)

    public suspend fun publishWithAck(topic: URI, arguments: Arguments, argumentsKw: ArgumentsKw): WampPublishResponse

    public suspend fun publish(topic: URI)

    public suspend fun publish(topic: URI, arguments: Arguments)

    public suspend fun publish(topic: URI, argumentsKw: ArgumentsKw): Unit = publish(topic, emptyList(), argumentsKw)

    public suspend fun publish(topic: URI, arguments: Arguments, argumentsKw: ArgumentsKw)


    public suspend fun subscribe(topic: URI): WampSubscribeResponse

    public suspend fun unsubscribe(subscription: Id): WampUnsubscribeResponse
}