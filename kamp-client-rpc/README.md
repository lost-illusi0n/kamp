# kamp-client-rpc
An extension to kamp-client which adds Kotlin type-safety to the raw RPC api.

## Procedures
This module provides a fully-typed hierarchy of RPC input and output combinations. A procedure is the basis for all the other functions in this module and can be defined using one of the many convenience functions.

### NOTE
In the future the procedure hierarchy may change completely to abstract the idea of positional and keyword arguments! Possibly, positional arguments will be the default but an annotation may be used on a parameter to mark it as a keyword argument. 

## Example
```kotlin
@Serializable
data class NameInput(val name: String)

@Serializable
data class NameOutput(val response: String)

val nameProcedure = simpleProcedure<FooInput, FooOutput>(URI.unsafe("x.y.foo"))

val nameRegistration = calleeWampSession.register(nameProcedure) { (name) ->
    yield(FooOutput("hi $name!"))
}

val (response) = callerWampSession.call(nameProcedure, FooInput("lost"))

println(response)

calleeWampSession.unregister(nameRegistration)
```