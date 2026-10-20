plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// CI passes the workflow run number so every build installs over the previous one.
val ciVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
val phase = 15
val releaseChannel = System.getenv("RELEASE_CHANNEL") ?: "dev"
val signingPassword: String? = System.getenv("SIGNING_PASSWORD")?.takeIf { it.isNotBlank() }
val signingStore = rootProject.file("signing/release.p12")
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
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
            }
        }
    }

    packaging {
        jniLibs.useLegacyPackaging = false
    }
}

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
