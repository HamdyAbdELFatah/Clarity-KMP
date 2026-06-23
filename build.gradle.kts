plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.binary.compatibility.validator)
}

val releaseVersion = providers.gradleProperty("VERSION_NAME")
    .orElse(providers.environmentVariable("GITHUB_REF_NAME").map { it.removePrefix("v") })
    .getOrElse("0.1.0-SNAPSHOT")

allprojects {
    group = "com.hamdy.clarity"
    version = releaseVersion
}

apiValidation {
    ignoredProjects.add("composeApp")
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib.enabled = true
}

// Runs the consumer-tests project as a *separate* Gradle process. Gradle 9.x forbids the
// in-process GradleBuild approach (the nested build can't lock the file-hash cache the parent
// daemon already holds: "Cannot lock file hash cache ... as it has already been locked by this
// process"). Invoking the wrapper out-of-process sidesteps that and also matches how CI runs it.
tasks.register<Exec>("verifyPublishedConsumers") {
    dependsOn(
        ":clarity-kmp:publishAllPublicationsToProjectLocalRepository",
        ":clarity-kmp-compose:publishAllPublicationsToProjectLocalRepository",
    )

    val gradlew = if (System.getProperty("os.name").lowercase().contains("windows")) "gradlew.bat" else "gradlew"
    val wrapper = rootProject.file(gradlew).absolutePath

    // Resolve the Android SDK the same way the host build does (ANDROID_HOME / ANDROID_SDK_ROOT
    // env, then sdk.dir from local.properties) and forward it so the nested build doesn't need
    // its own local.properties.
    val localSdkDir = rootProject.file("local.properties").let { f ->
        if (f.exists()) java.util.Properties().apply { f.inputStream().use { load(it) } }
            .getProperty("sdk.dir") else null
    }
    val androidHome = providers.environmentVariable("ANDROID_HOME")
        .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
        .orElse(providers.provider { localSdkDir })
        .orNull

    workingDir = rootProject.file("consumer-tests")
    commandLine(wrapper, "-p", rootProject.file("consumer-tests").absolutePath, "build", "linkDebugFrameworkIosSimulatorArm64")

    if (!androidHome.isNullOrBlank()) {
        environment("ANDROID_HOME", androidHome)
    }
}

