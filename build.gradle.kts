plugins {
    val kotlin = "1.6.10"

    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") version kotlin apply false
}

group = "net.lostillusion"
version = "1.0-SNAPSHOT"