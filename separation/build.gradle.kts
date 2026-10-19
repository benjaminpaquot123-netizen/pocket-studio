plugins {
    alias(libs.plugins.android.library)
}

// ONNX Runtime ships its C/C++ headers and libonnxruntime.so inside the AAR (no prefab):
// unpack them for CMake. The same AAR packages the .so into the APK.
val ort: Configuration by configurations.creating
val ortDir = layout.buildDirectory.dir("onnxruntime")
val unpackOnnxRuntime by tasks.registering(Copy::class) {
    from({ zipTree(ort.singleFile) }) { include("headers/**", "jni/**") }
    into(ortDir)
}

android {
    namespace = "com.pocketstudio.separation"
    compileSdk = 37
    ndkVersion = "30.0.16248370"

    defaultConfig {
        minSdk = 31
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared", "-DORT_DIR=${ortDir.get().asFile.absolutePath}")
                cppFlags += listOf("-std=c++17", "-fexceptions")
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a") +
                if (providers.gradleProperty("emulatorAbi").isPresent) listOf("x86_64") else emptyList()
        }
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

tasks.matching { it.name.startsWith("configureCMake") || it.name.startsWith("buildCMake") }.configureEach {
    dependsOn(unpackOnnxRuntime)
}

dependencies {
    implementation(libs.onnxruntime.android)
    ort(libs.onnxruntime.android) { artifact { type = "aar" } }
}
