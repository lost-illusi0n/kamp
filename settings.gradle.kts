rootProject.name = "kamp"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("kamp-client")
include("kamp-client-rpc")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")