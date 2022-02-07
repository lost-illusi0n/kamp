package net.lostillusion.kamp

import io.ktor.util.network.*
import kotlinx.coroutines.flow.MutableSharedFlow

public data class DefaultSessionData(
    val address: NetworkAddress,
    val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow()
)

