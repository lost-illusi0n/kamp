plugins {
    `kamp-module`
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.kampClient)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}