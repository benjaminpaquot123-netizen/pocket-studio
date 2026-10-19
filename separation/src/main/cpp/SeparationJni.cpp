#include <jni.h>

#include <algorithm>
#include <thread>

#include "Separator.h"

extern "C" {

// Returns the SeparationResult code; `listener` gets onProgress(float): Boolean (false = cancel).
// Worker thread only: this takes minutes.
JNIEXPORT jint JNICALL Java_com_pocketstudio_separation_Demucs_nativeSeparate(JNIEnv* env, jobject, jstring model,
                                                                            jfloatArray audio, jstring outDir,
                                                                            jobject listener) {
    const char* modelPath = env->GetStringUTFChars(model, nullptr);
    const char* dir = env->GetStringUTFChars(outDir, nullptr);
    jclass cls = env->GetObjectClass(listener);
    jmethodID onProgress = env->GetMethodID(cls, "onProgress", "(F)Z");
    if (!onProgress) {
        // Renamed by a shrinker or wrong listener: report instead of a JNI abort mid-run.
        env->ExceptionClear();
        env->ReleaseStringUTFChars(model, modelPath);
        env->ReleaseStringUTFChars(outDir, dir);
        return static_cast<jint>(ps::SeparationResult::ModelError);
    }
    const jsize length = env->GetArrayLength(audio);
    std::vector<float> samples(static_cast<size_t>(length));
    env->GetFloatArrayRegion(audio, 0, length, samples.data());
    // Big cores only would be ideal; half the cores keeps the phone usable meanwhile.
    const int threads = std::max(2, static_cast<int>(std::thread::hardware_concurrency()) / 2);
    const auto result = ps::separate(modelPath, samples.data(), length / 2, dir,
                                     [&](float p) {
                                         const jboolean keep = env->CallBooleanMethod(listener, onProgress, p);
                                         if (env->ExceptionCheck()) {
                                             env->ExceptionClear();
                                             return false;
                                         }
                                         return keep == JNI_TRUE;
                                     },
                                     threads);
    env->ReleaseStringUTFChars(model, modelPath);
    env->ReleaseStringUTFChars(outDir, dir);
    return static_cast<jint>(result.code);
}

// Same, from a raw interleaved float file written by the app's decoder (keeps songs off the Java heap).
JNIEXPORT jint JNICALL Java_com_pocketstudio_separation_Demucs_nativeSeparateFile(JNIEnv* env, jobject, jstring model,
                                                                                jstring raw, jint channels,
                                                                                jint sampleRate, jstring outDir,
                                                                                jobject listener) {
    const char* modelPath = env->GetStringUTFChars(model, nullptr);
    const char* rawPath = env->GetStringUTFChars(raw, nullptr);
    const char* dir = env->GetStringUTFChars(outDir, nullptr);
    jclass cls = env->GetObjectClass(listener);
    jmethodID onProgress = env->GetMethodID(cls, "onProgress", "(F)Z");
    if (!onProgress) {
        // Renamed by a shrinker or wrong listener: report instead of a JNI abort mid-run.
        env->ExceptionClear();
        env->ReleaseStringUTFChars(model, modelPath);
        env->ReleaseStringUTFChars(raw, rawPath);
        env->ReleaseStringUTFChars(outDir, dir);
        return static_cast<jint>(ps::SeparationResult::ModelError);
    }
    const int threads = std::max(2, static_cast<int>(std::thread::hardware_concurrency()) / 2);
    const auto result = ps::separateFile(modelPath, rawPath, channels, sampleRate, dir,
                                         [&](float p) {
                                             const jboolean keep = env->CallBooleanMethod(listener, onProgress, p);
                                             if (env->ExceptionCheck()) {
                                                 env->ExceptionClear();
                                                 return false;
                                             }
                                             return keep == JNI_TRUE;
                                         },
                                         threads);
    env->ReleaseStringUTFChars(model, modelPath);
    env->ReleaseStringUTFChars(raw, rawPath);
    env->ReleaseStringUTFChars(outDir, dir);
    return static_cast<jint>(result.code);
}

}  // extern "C"
