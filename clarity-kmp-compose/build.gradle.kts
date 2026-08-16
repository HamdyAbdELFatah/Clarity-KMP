plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    explicitApi()

    android {
        namespace = "com.hamdy.clarity.compose"
        compileSdk = 36
        minSdk = 24

        // Ship ProGuard/R8 keep rules for the transitive Microsoft Clarity SDK so that
        // consumers do not need to know the underlying dependency to keep it safe in
        // release builds.
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-rules.pro")
            }
        }

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }

        // Host-side (unit) tests are disabled by default with the new KMP plugin; opt in here.
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ClarityKmpCompose"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":clarity-kmp")) {
                exclude(group = "com.microsoft.clarity", module = "clarity")
            }
            api(libs.compose.runtime)
            implementation(libs.compose.foundation)
            api(libs.compose.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            // clarity-compose bundles the same runtime classes as clarity; exclude the standalone
            // clarity artifact here so consumers don't hit duplicate-class errors when both
            // :clarity-kmp (which brings clarity) and :clarity-kmp-compose are on the classpath.
            implementation("com.microsoft.clarity:clarity-compose:${libs.versions.clarity.sdk.get()}") {
                exclude(group = "com.microsoft.clarity", module = "clarity")
            }
        }

        // androidHostTest is created lazily by the KMP plugin after withHostTest {} runs,
        // so reference it by name rather than the typed accessor.
        val androidHostTest by getting
        androidHostTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.robolectric)
            implementation(libs.compose.ui.test)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "ProjectLocal"
            url = rootProject.layout.buildDirectory.dir("project-local-repository").get().asFile.toURI()
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "io.github.hamdyabdelfatah",
        artifactId = "clarity-kmp-compose",
        version = project.version.toString()
    )

    pom {
        name.set("Clarity KMP Compose")
        description.set("Unofficial Kotlin Multiplatform Compose bindings for Microsoft Clarity.")
        url.set("https://github.com/HamdyAbdELFatah/Clarity-KMP")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("hamdy")
                name.set("Hamdy Abd-ElFattah")
            }
        }

        scm {
            url.set("https://github.com/HamdyAbdELFatah/Clarity-KMP")
            connection.set("scm:git:git://github.com/HamdyAbdELFatah/Clarity-KMP.git")
            developerConnection.set("scm:git:ssh://github.com/HamdyAbdELFatah/Clarity-KMP.git")
        }
    }
}
