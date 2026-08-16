import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.dokka)
}

kotlin {
    explicitApi()

    android {
        namespace = "com.hamdy.clarity"
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
            baseName = "ClarityKmp"
            isStatic = true
        }
        iosTarget.binaries.all {
            if (this is org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable) {
                linkerOpts("-undefined", "dynamic_lookup")
            }
        }
        iosTarget.compilations.getByName("main") {
            val clarityInterop by cinterops.creating {
                definitionFile.set(project.file("src/nativeInterop/cinterop/clarity.def"))
                includeDirs(project.file("src/nativeInterop/cinterop/headers"))
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // No dependencies in commonMain — intentionally dependency-free.
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.clarity.sdk)
        }

        // androidHostTest is created lazily by the KMP plugin after withHostTest {} runs,
        // so reference it by name rather than the typed accessor.
        val androidHostTest by getting
        androidHostTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.robolectric)
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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "io.github.hamdyabdelfatah",
        artifactId = "clarity-kmp",
        version = project.version.toString()
    )

    pom {
        name.set("Clarity KMP")
        description.set("Unofficial Kotlin Multiplatform wrapper for Microsoft Clarity.")
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
