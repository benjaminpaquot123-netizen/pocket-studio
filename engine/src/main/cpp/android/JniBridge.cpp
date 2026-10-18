#include <jni.h>

#include <memory>
#include <mutex>

#include "AudioHost.h"

namespace {

std::mutex g_lock;
std::unique_ptr<ps::AudioHost> g_host;

ps::AudioHost& host() {
    std::lock_guard<std::mutex> guard(g_lock);
    if (!g_host) g_host = std::make_unique<ps::AudioHost>();
    return *g_host;
}

}  // namespace

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_start(JNIEnv*, jobject) {
    return host().start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_stop(JNIEnv*, jobject) {
    host().stop();
}

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_post(JNIEnv*, jobject, jint type,
                                                                          jfloat value) {
    return host().post({static_cast<ps::CommandType>(type), value}) ? JNI_TRUE : JNI_FALSE;
}

// Layout must match NativeEngine.StreamInfo.fromArray.
JNIEXPORT jdoubleArray JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeStreamInfo(JNIEnv* env, jobject) {
    auto& h = host();
    const auto info = h.info();
    const double values[] = {
        static_cast<double>(info.sampleRate),
        static_cast<double>(info.framesPerBurst),
        static_cast<double>(info.bufferSize),
        static_cast<double>(info.bufferCapacity),
        info.exclusive ? 1.0 : 0.0,
        info.aaudio ? 1.0 : 0.0,
        info.mmap ? 1.0 : 0.0,
        info.lowLatency ? 1.0 : 0.0,
        static_cast<double>(info.xruns),
        info.outputLatencyMs,
        info.running ? 1.0 : 0.0,
        static_cast<double>(h.engine().callbackCount()),
        static_cast<double>(h.engine().takePeak()),
        h.engine().hasNonFinite() ? 1.0 : 0.0,
    };
    const jsize count = sizeof(values) / sizeof(values[0]);
    jdoubleArray array = env->NewDoubleArray(count);
    env->SetDoubleArrayRegion(array, 0, count, values);
    return array;
}

// Returns an AudioHost::LatencyStart code (0 = started).
JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeStartLatencyTest(JNIEnv*, jobject) {
    return static_cast<jint>(host().startLatencyTest());
}

JNIEXPORT jstring JNICALL Java_com_pocketstudio_engine_NativeEngine_inputDescription(JNIEnv* env, jobject) {
    return env->NewStringUTF(host().inputDescription());
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_latencyTestState(JNIEnv*, jobject) {
    return static_cast<jint>(host().engine().latencyMeter().state());
}

// Returns [valid, latencyMs, confidence, inputPeak, inputFrames, failure] and closes the input stream.
JNIEXPORT jdoubleArray JNICALL Java_com_pocketstudio_engine_NativeEngine_finishLatencyTest(JNIEnv* env, jobject) {
    auto& h = host();
    ps::RoundTripMeter::Result result;
    if (h.engine().latencyMeter().state() == ps::RoundTripMeter::State::Done) {
        result = h.engine().latencyMeter().analyze();
    }
    h.finishLatencyTest();
    const double values[] = {
        result.valid ? 1.0 : 0.0,
        result.latencyMs,
        result.confidence,
        static_cast<double>(result.inputPeak),
        static_cast<double>(result.inputFrames),
        static_cast<double>(result.failure),
    };
    jdoubleArray array = env->NewDoubleArray(6);
    env->SetDoubleArrayRegion(array, 0, 6, values);
    return array;
}

}  // extern "C"
