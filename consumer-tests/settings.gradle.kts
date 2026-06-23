pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("../build/project-local-repository") }
        google()
        mavenCentral()
    }
}

rootProject.name = "clarity-kmp-consumer-test"
