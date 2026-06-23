pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Clarity-KMP"

include(":clarity-kmp")
include(":clarity-kmp-compose")
include(":sample:composeApp")
include(":sample:androidApp")
