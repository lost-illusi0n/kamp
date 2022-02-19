plugins {
    `kamp-module`
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.kampClient)
                implementation(libs.kotlinx.coroutines)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}