plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.hamdy.clarity.sample.shared"
        compileSdk = 36
        minSdk = 24
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":clarity-kmp-compose"))
            // The deprecated compose.* accessors still resolve through the Compose Multiplatform
            // plugin's repositories/BOM, while the equivalent version-catalog coordinates are not
            // reliably available on mavenCentral() for all artifacts. Keep them here in the sample.
            @Suppress("DEPRECATION")
            implementation(compose.runtime)
            @Suppress("DEPRECATION")
            implementation(compose.foundation)
            @Suppress("DEPRECATION")
            implementation(compose.material3)
            @Suppress("DEPRECATION")
            implementation(compose.ui)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}
