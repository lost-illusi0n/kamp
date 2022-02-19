# kamp-client-rpc
An extension to kamp-client which adds Kotlin features over the raw pubsub api.

## Events
An event is the data you can either subscribe to or publish to. An event is the basis for all the other functions in this module and can be defined using one of the many convenience functions.

### NOTE
In the future the procedure hierarchy may change completely to abstract the idea of positional and keyword arguments! Possibly, positional arguments will be the default but an annotation may be used on a parameter to mark it as a keyword argument. 

## Example
```kotlin
@Serializable
data class CommentEventData(val name: String, val comment: String)

val commentEvent = singleEvent<CommentEventData>(URI.unsafe("x.y.comments"))

val commentSubscription = subscriberSession.subscribe(commentEvent) { (name, comment) ->
    println("[SESSION 1] $name: $comment")
}

val anotherCommentSubscription = anotherSubscriberSession.subscribe(commentEvent) { (name, comment) ->
    println("[SESSION 2] $name: $comment")
}

publisherSession.publish(commentEvent, CommentEventData("Lost", "Wow, KAMP is so good!"))
```