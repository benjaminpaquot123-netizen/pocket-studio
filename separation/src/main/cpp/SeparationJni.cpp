#include <jni.h>

#include <algorithm>
#include <new>
#include <string>
#include <thread>
#include <vector>

#include "PitchTranscriber.h"
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

// Basic Pitch transcription. Returns [start, end, pitch, amplitude] * N (seconds, MIDI note,
// 0..1), or null with a pending IllegalStateException carrying the reason. Worker thread only.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_separation_BasicPitch_nativeTranscribe(JNIEnv* env, jobject,
                                                                                         jstring model,
                                                                                         jfloatArray mono,
                                                                                         jint sampleRate) {
    auto fail = [&](const std::string& message) -> jfloatArray {
        if (env->ExceptionCheck()) return nullptr;
        jclass ise = env->FindClass("java/lang/IllegalStateException");
        if (ise) env->ThrowNew(ise, message.c_str());
        return nullptr;
    };
    if (!model || !mono) return fail("son absent");
    const char* modelPath = env->GetStringUTFChars(model, nullptr);
    if (!modelPath) return nullptr;  // OutOfMemoryError pending
    const std::string path(modelPath);
    env->ReleaseStringUTFChars(model, modelPath);
    std::vector<float> samples;
    std::vector<ps::PitchNote> notes;
    std::string error;
    try {
        samples.resize(static_cast<size_t>(env->GetArrayLength(mono)));
        env->GetFloatArrayRegion(mono, 0, static_cast<jsize>(samples.size()), samples.data());
        if (env->ExceptionCheck()) return nullptr;
        notes = ps::transcribe(path, samples.data(), samples.size(), sampleRate, &error);
        std::vector<float>().swap(samples);
    } catch (const std::bad_alloc&) {
        return fail("mémoire insuffisante");
    }
    if (!error.empty()) return fail(error);
    const auto count = static_cast<jsize>(notes.size() * 4);
    jfloatArray result = env->NewFloatArray(count);
    if (!result) return nullptr;  // OutOfMemoryError pending
    std::vector<float> flat;
    flat.reserve(static_cast<size_t>(count));
    for (const auto& n : notes) {
        flat.push_back(static_cast<float>(n.start));
        flat.push_back(static_cast<float>(n.end));
        flat.push_back(static_cast<float>(n.pitch));
        flat.push_back(n.amplitude);
    }
    env->SetFloatArrayRegion(result, 0, count, flat.data());
    return result;
}

}  // extern "C"
