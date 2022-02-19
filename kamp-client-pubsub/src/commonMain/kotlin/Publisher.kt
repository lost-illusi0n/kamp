package net.lostillusion.kamp.client.pubsub

import net.lostillusion.kamp.client.WampPublishResponse
import net.lostillusion.kamp.client.WampSession

public suspend fun WampSession.publish(event: Event.Nothing) {
    publish(event.topic)
}

public suspend fun <E> WampSession.publish(event: Event.Single<E>, value: E) {
    publish(event.topic, event.convertFromEvent(value))
}

public suspend fun <E, EKw> WampSession.publish(event: Event.Full<E, EKw>, value: E, valueKw: EKw) {
    val (args, argsKw) = event.convertFromEvent(value, valueKw)

    publish(event.topic, args, argsKw)
}

public suspend fun WampSession.publishWithAck(event: Event.Nothing): WampPublishResponse = publishWithAck(event.topic)

public suspend fun <E> WampSession.publishWithAck(event: Event.Single<E>, value: E): WampPublishResponse =
    publishWithAck(event.topic, event.convertFromEvent(value))

public suspend fun <E, EKw> WampSession.publishWithAck(event: Event.Full<E, EKw>, value: E, valueKw: EKw): WampPublishResponse {
    val (args, argsKw) = event.convertFromEvent(value, valueKw)

    return publishWithAck(event.topic, args, argsKw)
}