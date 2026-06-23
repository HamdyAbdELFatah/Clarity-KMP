plugins {
    kotlin("multiplatform") version "2.3.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
    id("org.jetbrains.compose") version "1.11.0"
    id("com.android.kotlin.multiplatform.library") version "9.2.0"
}

kotlin {
    android {
        namespace = "com.hamdy.clarity.consumer"
        compileSdk = 36
        minSdk = 24
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ClarityConsumerTest"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.hamdy.clarity:clarity-kmp-compose:0.1.0-SNAPSHOT")
        }
    }
}
