package net.lostillusion.kamp.client.pubsub

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.lostillusion.kamp.client.*

public data class WampSubscription(val subscription: Id, val job: Job)

public data class WampEventScope(val publication: Id, val subscription: Id, val details: WampMessage.Event.Details)

internal suspend fun WampSession.subscribe(
    event: Event,
    onEvent: WampEventScope.(Arguments?, ArgumentsKw?) -> Unit
): WampSubscription {
    val response = subscribe(event.topic)

    if (response is WampMessage.SubscribeError) TODO()

    require(response is WampMessage.Subscribed)

    val job = incoming
        .filterIsInstance<WampMessage.Event>()
        .filter { it.subscription == response.subscription }
        .onEach { onEvent(WampEventScope(it.publication, it.subscription, it.details), it.arguments, it.argumentsKw) }
        .launchIn(scope)

    return WampSubscription(response.subscription, job)
}

public suspend fun WampSession.subscribe(event: Event.Nothing, onEvent: WampEventScope.() -> Unit): WampSubscription =
    subscribe(event) { _, _ -> onEvent(this) }

public suspend fun <E> WampSession.subscribe(event: Event.Single<E>, onEvent: WampEventScope.(E) -> Unit): WampSubscription =
    subscribe(event) { args, _ -> onEvent(event.convertToEvent(args!!)) }

public suspend fun <E, EKw> WampSession.subscribe(event: Event.Full<E, EKw>, onEvent: WampEventScope.(E, EKw) -> Unit): WampSubscription =
    subscribe(event) { args, argsKw ->
        val (event, eventKw) = event.convertToEvent(args!!, argsKw!!)

        onEvent(this, event, eventKw)
    }

public suspend fun WampSession.unsubscribe(subscription: WampSubscription): WampUnsubscribeResponse {
    val response = unsubscribe(subscription.subscription)
    subscription.job.cancelAndJoin()
    return response
}