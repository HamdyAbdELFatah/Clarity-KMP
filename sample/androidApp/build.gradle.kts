plugins {
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hamdy.clarity.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hamdy.clarity.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":sample:composeApp"))
    // :clarity-kmp brings com.microsoft.clarity:clarity, while :clarity-kmp-compose brings
    // com.microsoft.clarity:clarity-compose, which bundles the SAME runtime classes. Exclude the
    // standalone clarity artifact so the app doesn't see duplicate com.microsoft.clarity.* classes.
    implementation(project(":clarity-kmp")) {
        exclude(group = "com.microsoft.clarity", module = "clarity")
    }
    implementation(libs.androidx.activity.compose)
}
