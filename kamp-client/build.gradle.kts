plugins {
    `kamp-module`
    kotlin("plugin.serialization")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.bundles.client)
            }
        }
    }
}