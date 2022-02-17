import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()

    sourceSets.all {
        languageSettings.optIn("kotlin.RequiresOptIn")
        languageSettings.languageVersion = "1.6"
        languageSettings.apiVersion = "1.6"
    }
}
//
//tasks {
//    withType<KotlinCompile> {
//        kotlinOptions {
//            languageVersion = "1.6"
//            freeCompilerArgs = listOf(
//                "-opt-in=kotlin.RequiresOptIn"
//            )
//        }
//    }
//}