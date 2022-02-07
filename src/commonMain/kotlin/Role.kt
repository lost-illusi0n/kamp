package net.lostillusion.kamp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class Role {
    @SerialName("publisher") Publisher,
    @SerialName("subscriber") Subscriber,
    @SerialName("caller") Caller,
    @SerialName("callee") Callee,
    @SerialName("dealer") Dealer,
    @SerialName("broker") Broker
}