plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// CI passes the workflow run number so every build installs over the previous one.
val envVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull()
val ciVersionCode = envVersionCode ?: 1
val phase = 22
val releaseChannel = System.getenv("RELEASE_CHANNEL") ?: "dev"
val signingPassword: String? = System.getenv("SIGNING_PASSWORD")?.takeIf { it.isNotBlank() }
val signingStore = rootProject.file("signing/release.p12")
// A release APK must install over the published one: same key, higher versionCode. Without the
// secret and VERSION_CODE, a release build fails (checkReleaseSigning) unless it is asked for:
// -PunsignedRelease (CI: signed by apksigner in the publish job, the only one with the secret) or
// -PallowDebugSigning (a local check that the release builds; that APK must never reach the phone).
val unsignedRelease = providers.gradleProperty("unsignedRelease").isPresent
val allowDebugSigning = providers.gradleProperty("allowDebugSigning").isPresent
// The phone is arm64-v8a; x86_64 is only added for CI emulator smoke tests (-PemulatorAbi).
val abis = listOf("arm64-v8a") + if (providers.gradleProperty("emulatorAbi").isPresent) listOf("x86_64") else emptyList()

android {
    namespace = "com.pocketstudio.app"
    compileSdk = 37
    ndkVersion = "30.0.16248370"

    defaultConfig {
        applicationId = "com.pocketstudio"
        minSdk = 31
        targetSdk = 37
        versionCode = ciVersionCode
        versionName = "0.$phase.$ciVersionCode"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GITHUB_REPO", "\"benjaminpaquot123-netizen/pocket-studio\"")
        buildConfigField("String", "RELEASE_CHANNEL", "\"$releaseChannel\"")
        ndk {
            abiFilters += abis
        }
    }

    signingConfigs {
        if (signingPassword != null && signingStore.exists()) {
            create("release") {
                storeFile = signingStore
                storeType = "PKCS12"
                storePassword = signingPassword
                keyAlias = "pocketstudio"
                keyPassword = signingPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = when {
                unsignedRelease -> null
                else -> signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The users' paths (docs/TESTS-REELS.md): one copy for the emulator's tests and Robolectric's.
    sourceSets {
        getByName("test").kotlin.srcDir("src/sharedTest/java")
        getByName("androidTest").kotlin.srcDir("src/sharedTest/java")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                // SDK 36 Robolectric needs JDK internals on JDK 21+.
                it.jvmArgs(
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
                // The song bench (scripts/song-bench.sh): SongBenchExport records only when given -Pbench.out
                // (LiveSongRecorder, scripts/live-bench.sh: with -Pbench.live; -Pbench.loop for the soak).
                // (bench.sublayers=1: the bass music subs as layers of their bass, G1b SubLayers.)
                // (bench.guide=accept: each song followed to the end of its Guide's parcours first, G6.)
                // RealProjectsTest (docs/TESTS-REELS.md): -Preal.projects, -Preal.out, -Preal.live, -Preal.render.
                for (key in listOf("bench.out", "bench.render", "bench.styles", "bench.guide", "bench.live", "bench.seconds", "bench.loop", "bench.sublayers", "real.projects", "real.out", "real.live", "real.render")) {
                    providers.gradleProperty(key).orNull?.let { value -> it.systemProperty("ps.$key", value) }
                }
                // Every style's song in one run (or every preview, PreviewLevelBench): more room than the tests need.
                if (listOf("bench.live", "bench.out", "bench.render").any { k -> providers.gradleProperty(k).isPresent }) it.maxHeapSize = "3g"
            }
        }
    }

    packaging {
        jniLibs.useLegacyPackaging = false
    }
}

val checkReleaseSigning = tasks.register("checkReleaseSigning") {
    val signed = signingPassword != null && signingStore.exists()
    val hasVersionCode = envVersionCode != null
    val unsigned = unsignedRelease
    val debugAllowed = allowDebugSigning
    doLast {
        val problems = buildList {
            if (!signed && !unsigned && !debugAllowed) add("no SIGNING_PASSWORD: the APK would be signed with the debug key and refuse to install over the published app")
            if (!hasVersionCode && !debugAllowed) add("no VERSION_CODE: versionCode would be 1, lower than every published version")
        }
        if (problems.isNotEmpty()) throw GradleException(
            "Release build refused: " + problems.joinToString("; ") +
                ". Set SIGNING_PASSWORD and VERSION_CODE (scripts/local-release.sh), or pass -PunsignedRelease (CI, signed later)" +
                " or -PallowDebugSigning (local check only, never install that APK).",
        )
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkReleaseSigning) }

roborazzi {
    outputDir.set(file("screenshots"))
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":separation"))
    implementation(project(":ui-components"))
    implementation(project(":project-model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
