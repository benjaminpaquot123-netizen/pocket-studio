plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pocketstudio.engine"
    compileSdk = 37
    ndkVersion = "30.0.16248370"

    defaultConfig {
        minSdk = 31
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared")
                cppFlags += listOf("-std=c++20")
            }
        }
        ndk {
            // arm64-v8a ships to the phone; x86_64 only for emulator smoke tests (-PemulatorAbi).
            abiFilters += listOf("arm64-v8a") +
                if (providers.gradleProperty("emulatorAbi").isPresent) listOf("x86_64") else emptyList()
        }
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        prefab = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.oboe)
}
