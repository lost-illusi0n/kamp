package net.lostillusion.kamp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
public value class URI private constructor(public val value: String) {
    public companion object {
        // these two may need to change with an advanced profile implementation
        private val LOOSE_PATTERN = Regex("^([^\\s\\.#]+\\.)*([^\\s\\.#]+)\$")
        private val STRICT_PATTERN = Regex("^([0-9a-z_]+\\.)*([0-9a-z_]+)\$")

        public fun loose(value: String): URI? {
            if(value.startsWith("wamp.") || !LOOSE_PATTERN.matches(value)) return null

            return URI(value)
        }

        public fun strict(value: String): URI? {
            if(value.startsWith("wamp.") || !STRICT_PATTERN.matches(value)) return null

            return URI(value)
        }

        /**
         * Creates a [URI] from [value] without any checks.
         * Use this only when using known good values that follow the URI spec.
         */
        public fun unsafe(value: String): URI = URI(value)
    }
}

internal val String.uri: URI
    get() = URI.unsafe(this)