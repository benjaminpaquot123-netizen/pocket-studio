#include <jni.h>

#include <algorithm>
#include <chrono>
#include <thread>

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <memory>
#include <vector>
#include <mutex>

#include "Analysis.h"
#include "AudioHost.h"
#include "OfflineRenderer.h"
#include "OnsetDetector.h"
#include "Sample.h"
#include "SoundFontPlayer.h"
#include "TimeStretch.h"

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

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeRetryOutput(JNIEnv*, jobject) {
    // Never creates the host (the device callback fires once at the app's start, before any sound).
    std::lock_guard<std::mutex> guard(g_lock);
    if (g_host) g_host->retryOutput();
}

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativePost(JNIEnv*, jobject, jint type,
                                                                          jfloat value, jint a, jint b) {
    return host().post({static_cast<ps::CommandType>(type), value, a, b}) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeCurrentStep(JNIEnv*, jobject) {
    return host().engine().currentStep();
}

// The audio thread's smoothed load (1 = no time to spare). Any thread.
JNIEXPORT jfloat JNICALL Java_com_pocketstudio_engine_NativeEngine_cpuLoad(JNIEnv*, jobject) {
    return host().engine().cpuLoad();
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_setBufferBursts(JNIEnv*, jobject, jint bursts) {
    host().setBufferBursts(bursts);
}

JNIEXPORT jdouble JNICALL Java_com_pocketstudio_engine_NativeEngine_nativePlayPosition(JNIEnv*, jobject) {
    return host().engine().playPosition();
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeAbsoluteStep(JNIEnv*, jobject) {
    return host().engine().absoluteStep();
}

// --- Samples: created and released off the audio thread, shared by reference. ---

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_sampleCreate(JNIEnv* env, jobject, jfloatArray data,
                                                                             jint channels, jint sampleRate) {
    const jsize length = env->GetArrayLength(data);
    if (channels < 1 || length < channels) return 0;
    jfloat* in = env->GetFloatArrayElements(data, nullptr);
    if (!in) return 0;
    ps::Sample* s = nullptr;
    try {
        s = ps::Sample::create(in, length / channels, channels, sampleRate, 48000);
    } catch (...) {  // out of memory (a long sound resampled): no sample rather than a crash through JNI
        s = nullptr;
    }
    env->ReleaseFloatArrayElements(data, in, JNI_ABORT);
    return reinterpret_cast<jlong>(s);
}

// The same, kept as 16-bit integers (half the memory): a zone of a multizone instrument (phase 38).
JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_sampleCreateCompact(JNIEnv* env, jobject, jfloatArray data,
                                                                                    jint channels, jint sampleRate) {
    const jsize length = env->GetArrayLength(data);
    if (channels < 1 || length < channels) return 0;
    jfloat* in = env->GetFloatArrayElements(data, nullptr);
    if (!in) return 0;
    ps::Sample* s = nullptr;
    try {
        s = ps::Sample::createCompact(in, length / channels, channels, sampleRate, 48000);
    } catch (...) {  // out of memory (an instrument's zones): the zone is refused, no crash through JNI
        s = nullptr;
    }
    env->ReleaseFloatArrayElements(data, in, JNI_ABORT);
    return reinterpret_cast<jlong>(s);
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeCollectGarbage(JNIEnv*, jobject) {
    host().engine().collectGarbage();
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_sampleRelease(JNIEnv*, jobject, jlong handle) {
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (s && s->release()) delete s;
}

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_sampleFrames(JNIEnv*, jobject, jlong handle) {
    return reinterpret_cast<ps::Sample*>(handle)->frames();
}

// Attack positions (fractions 0..1 of the sound), first is always 0. Worker thread.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_sampleOnsets(JNIEnv* env, jobject, jlong handle,
                                                                                   jfloat sensitivity, jint maxSlices) {
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (s && s->compact()) return env->NewFloatArray(0);  // a multizone zone: only MultiSampler reads 16-bit samples
    std::vector<float> mono(static_cast<size_t>(s->frames()));
    const int ch = s->channels();
    for (int64_t i = 0; i < s->frames(); ++i) {
        mono[static_cast<size_t>(i)] = ch == 2 ? 0.5f * (s->data()[i * 2] + s->data()[i * 2 + 1]) : s->data()[i];
    }
    ps::OnsetOptions options;
    options.sensitivity = sensitivity;
    options.maxSlices = maxSlices;
    const auto onsets = ps::detectOnsets(mono.data(), s->frames(), 48000, options);
    std::vector<jfloat> fractions;
    for (auto f : onsets) fractions.push_back(static_cast<jfloat>(static_cast<double>(f) / static_cast<double>(s->frames())));
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(fractions.size()));
    if (result) env->SetFloatArrayRegion(result, 0, static_cast<jsize>(fractions.size()), fractions.data());
    return result;
}

// Tempo of a WAV file: [bpm, confidence, firstBeat]. Worker thread.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_analyzeTempo(JNIEnv* env, jobject, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    if (!p) return nullptr;
    ps::TempoResult r;
    try {
        r = ps::detectTempo(std::string(p));
    } catch (...) {  // out of memory: no tempo rather than a crash
    }
    env->ReleaseStringUTFChars(path, p);
    const jfloat values[3] = {r.bpm, r.confidence, r.firstBeat};
    jfloatArray result = env->NewFloatArray(3);
    if (result) env->SetFloatArrayRegion(result, 0, 3, values);
    return result;
}

// Key over several WAV files: [tonic, minor, confidence]. Worker thread.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_analyzeKey(JNIEnv* env, jobject, jobjectArray paths) {
    std::vector<std::string> files;
    const jsize n = env->GetArrayLength(paths);
    for (jsize i = 0; i < n; ++i) {
        auto s = static_cast<jstring>(env->GetObjectArrayElement(paths, i));
        if (!s) continue;
        const char* p = env->GetStringUTFChars(s, nullptr);
        if (p) files.emplace_back(p);
        if (p) env->ReleaseStringUTFChars(s, p);
        env->DeleteLocalRef(s);
    }
    ps::KeyResult r;
    try {
        r = ps::detectKey(files);
    } catch (...) {
    }
    const jfloat values[3] = {static_cast<jfloat>(r.tonic), r.minor ? 1.0f : 0.0f, r.confidence};
    jfloatArray result = env->NewFloatArray(3);
    if (result) env->SetFloatArrayRegion(result, 0, 3, values);
    return result;
}

// A new sample: the region [start, end) of `handle` stretched to `outFrames`, transposed. Worker thread.
// « Recaler sur le tempo » (phase 35): a copy of `data` (interleaved, `channels`) lasting
// `outFrames`, pitch kept. Worker thread (seconds for a long take); null when it cannot be made.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_stretchPcm(JNIEnv* env, jobject, jfloatArray data,
                                                                               jint channels, jlong outFrames, jint sampleRate) {
    if (data == nullptr || channels < 1 || channels > 2) return nullptr;
    const int rate = std::clamp<jint>(sampleRate, 8000, 192000);
    const int64_t frames = env->GetArrayLength(data) / channels;
    // An audio track holds five minutes at most: room for a slower tempo, never the whole heap.
    if (frames < 2 || outFrames < 2 || outFrames > static_cast<int64_t>(rate) * 420) return nullptr;
    std::vector<float> out;
    // The take itself (no copy when the runtime lends it): one copy less at the peak.
    jfloat* in = env->GetFloatArrayElements(data, nullptr);
    if (!in) return nullptr;
    try {
        out = ps::timeStretch(in, frames, channels, outFrames, 0.0f, rate);
    } catch (...) {
        out.clear();
    }
    env->ReleaseFloatArrayElements(data, in, JNI_ABORT);
    if (out.empty()) return nullptr;
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(out.size()));
    if (result) env->SetFloatArrayRegion(result, 0, static_cast<jsize>(out.size()), out.data());
    return result;
}

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_sampleStretched(JNIEnv*, jobject, jlong handle, jfloat start,
                                                                                jfloat end, jlong outFrames, jfloat semitones) {
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (s && s->compact()) return 0;  // a multizone zone: only MultiSampler reads 16-bit samples
    const int64_t from = std::clamp<int64_t>(static_cast<int64_t>(start * s->frames()), 0, s->frames());
    const int64_t to = std::clamp<int64_t>(static_cast<int64_t>(end * s->frames()), from, s->frames());
    if (to - from < 2 || outFrames < 2 || outFrames > 48000LL * 120) return 0;
    const auto out = ps::timeStretch(s->data() + from * s->channels(), to - from, s->channels(), outFrames, semitones, 48000);
    if (out.empty()) return 0;
    return reinterpret_cast<jlong>(ps::Sample::create(out.data(), outFrames, s->channels(), 48000, 48000));
}

// The posted command owns one reference; it is given back if the queue is full.
JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeInstallSample(JNIEnv*, jobject, jint pad,
                                                                                       jlong handle) {
    auto& engine = host().engine();
    engine.collectGarbage();
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (s && s->compact()) return JNI_FALSE;  // a multizone zone: only MultiSampler reads 16-bit samples
    if (s) s->retain();
    ps::Command command{ps::CommandType::InstallSample, 0.0f, pad, 0};
    command.sample = s;
    if (host().post(command)) return JNI_TRUE;
    if (s && s->release()) delete s;
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportInstallSample(JNIEnv*, jobject, jlong exportHandle,
                                                                                   jint pad, jlong handle) {
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (s && s->compact()) return;  // a multizone zone: only MultiSampler reads 16-bit samples
    if (s) s->retain();
    ps::Command command{ps::CommandType::InstallSample, 0.0f, pad, 0};
    command.sample = s;
    reinterpret_cast<ps::OfflineRenderer*>(exportHandle)->post(command);
}

// --- Remix stems. Load on a worker thread (seconds); install on the main thread. ---

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_stemsLoad(JNIEnv* env, jobject, jobjectArray paths) {
    std::vector<std::string> files;
    const jsize n = env->GetArrayLength(paths);
    for (jsize i = 0; i < n; ++i) {
        auto s = static_cast<jstring>(env->GetObjectArrayElement(paths, i));
        if (!s) return 0;
        const char* p = env->GetStringUTFChars(s, nullptr);
        if (!p) return 0;
        files.emplace_back(p);
        env->ReleaseStringUTFChars(s, p);
        env->DeleteLocalRef(s);
    }
    try {
        return reinterpret_cast<jlong>(ps::StemBundle::load(files, 48000));
    } catch (...) {
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_stemsRelease(JNIEnv*, jobject, jlong handle) {
    auto* b = reinterpret_cast<ps::StemBundle*>(handle);
    if (b && b->release()) delete b;
}

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_stemsFrames(JNIEnv*, jobject, jlong handle) {
    return static_cast<jlong>(reinterpret_cast<ps::StemBundle*>(handle)->frames());
}

JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_stemsExtract(JNIEnv* env, jobject, jlong handle, jint stem,
                                                                                  jlong start, jlong count) {
    // Pads hold at most a minute: cap the copy so a bad request cannot exhaust the Java heap.
    std::vector<float> region;
    try {
        region = reinterpret_cast<ps::StemBundle*>(handle)->extract(stem, start, std::min<jlong>(count, 48000LL * 60));
    } catch (...) {
        region.clear();
    }
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(region.size()));
    if (result) env->SetFloatArrayRegion(result, 0, static_cast<jsize>(region.size()), region.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_stemsPeaks(JNIEnv* env, jobject, jlong handle, jint stem,
                                                                                jint buckets) {
    std::vector<float> peaks;
    try {
        peaks = reinterpret_cast<ps::StemBundle*>(handle)->peaks(stem, std::clamp(buckets, 1, 4096));
    } catch (...) {
        peaks.clear();
    }
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(peaks.size()));
    if (result) env->SetFloatArrayRegion(result, 0, static_cast<jsize>(peaks.size()), peaks.data());
    return result;
}

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_stemsStretched(JNIEnv*, jobject, jlong handle, jdouble ratio) {
    try {
        return reinterpret_cast<jlong>(reinterpret_cast<ps::StemBundle*>(handle)->stretched(ratio, 48000));
    } catch (const std::bad_alloc&) {
        return 0;
    }
}

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeInstallStems(JNIEnv*, jobject, jlong handle) {
    auto& engine = host().engine();
    engine.collectGarbage();
    auto* b = reinterpret_cast<ps::StemBundle*>(handle);
    if (b) b->retain();
    ps::Command command{ps::CommandType::InstallStems};
    command.stems = b;
    if (host().post(command)) return JNI_TRUE;
    if (b && b->release()) delete b;
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportInstallStems(JNIEnv*, jobject, jlong exportHandle, jlong handle) {
    auto* b = reinterpret_cast<ps::StemBundle*>(handle);
    if (b) b->retain();
    ps::Command command{ps::CommandType::InstallStems};
    command.stems = b;
    reinterpret_cast<ps::OfflineRenderer*>(exportHandle)->post(command);
}

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeInstallPreviewStems(JNIEnv*, jobject, jlong handle) {
    auto& engine = host().engine();
    engine.collectGarbage();
    auto* b = reinterpret_cast<ps::StemBundle*>(handle);
    if (b) b->retain();
    ps::Command command{ps::CommandType::InstallStems, 0.0f, 1};
    command.stems = b;
    if (host().post(command)) return JNI_TRUE;
    if (b && b->release()) delete b;
    return JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_previewPosition(JNIEnv*, jobject) {
    return static_cast<jlong>(host().engine().previewPosition());
}

// --- Audio tracks: clips and microphone recording ---

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeInstallAudioClip(JNIEnv*, jobject, jint track, jlong handle) {
    host().engine().collectGarbage();
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (s && s->compact()) return JNI_FALSE;  // a multizone zone: only MultiSampler reads 16-bit samples
    if (s) s->retain();
    ps::Command command{ps::CommandType::InstallAudioClip, 0.0f, track, 0};
    command.sample = s;
    if (host().post(command)) return JNI_TRUE;
    if (s && s->release()) delete s;
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportInstallAudioClip(JNIEnv*, jobject, jlong exportHandle, jint track,
                                                                                    jlong handle) {
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (s && s->compact()) return;  // a multizone zone: only MultiSampler reads 16-bit samples
    if (s) s->retain();
    ps::Command command{ps::CommandType::InstallAudioClip, 0.0f, track, 0};
    command.sample = s;
    reinterpret_cast<ps::OfflineRenderer*>(exportHandle)->post(command);
}

// --- Instrument tracks: the sample played across the keys ---

// `track` | (zone + 1) << 8: a zone of the track's multizone instrument (phase 38); the plain
// track index: its sampled source. A 16-bit (compact) sample may only go to a zone.
static int32_t trackSampleArg(jint track) {
    // G1b: a layer's sound (ps::layerSound) as well as a track's.
    return (std::clamp<jint>(track & 0xFF, 0, ps::kSounds - 1) << 16) | ((track >> 8) & 0xFFFF);
}
static bool refusedOnTrack(jint track, ps::Sample* s) { return s && s->compact() && ((track >> 8) & 0xFFFF) == 0; }

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeInstallTrackSample(JNIEnv*, jobject, jint track, jlong handle) {
    host().engine().collectGarbage();
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (refusedOnTrack(track, s)) return JNI_FALSE;
    if (s) s->retain();
    ps::Command command{ps::CommandType::InstallTrackSample, 0.0f, trackSampleArg(track), 0};
    command.sample = s;
    if (host().post(command)) return JNI_TRUE;
    if (s && s->release()) delete s;
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportInstallTrackSample(JNIEnv*, jobject, jlong exportHandle, jint track,
                                                                                      jlong handle) {
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (refusedOnTrack(track, s)) return;
    if (s) s->retain();
    ps::Command command{ps::CommandType::InstallTrackSample, 0.0f, trackSampleArg(track), 0};
    command.sample = s;
    reinterpret_cast<ps::OfflineRenderer*>(exportHandle)->post(command);
}

// Takes the engine had not let go of yet (stream stopped or restarting): freed once it has.
static std::mutex zombieLock;
static std::vector<ps::RecordBuffer*> zombies;

static void freeReleasedTakes() {
    std::lock_guard<std::mutex> guard(zombieLock);
    zombies.erase(std::remove_if(zombies.begin(), zombies.end(), [](ps::RecordBuffer* b) {
        if (b->state.load(std::memory_order_acquire) != 2) return false;
        delete b;
        return true;
    }), zombies.end());
}

// Opens the microphone and arms a recording of up to `seconds`; 0 when the microphone is unavailable.
// `onStart`: the recording begins exactly when the transport reaches its first step; the last
// `prerollFrames` before it (the count-in) are kept in front of it.
JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_recordStart(JNIEnv*, jobject, jboolean onStart, jint seconds, jint prerollFrames) {
    if (seconds <= 0 || seconds > 600) return 0;
    freeReleasedTakes();
    if (!host().startRecordingInput(ps::AudioHost::MicRecording)) return 0;
    const int64_t preroll = onStart ? std::clamp<int64_t>(prerollFrames, 0, 48000 * 4) : 0;
    ps::RecordBuffer* buffer = nullptr;
    try {
        buffer = new ps::RecordBuffer(static_cast<int64_t>(seconds) * 48000, preroll);
    } catch (const std::bad_alloc&) {
        // Out of memory (big instruments loaded): no take, as with no microphone, never a crash.
        host().stopRecordingInput(ps::AudioHost::MicRecording);
        return 0;
    }
    ps::Command command{ps::CommandType::RecordArm, 0.0f, onStart ? 1 : 0, 0};
    command.record = buffer;
    if (!host().post(command)) {
        delete buffer;
        host().stopRecordingInput(ps::AudioHost::MicRecording);
        return 0;
    }
    return reinterpret_cast<jlong>(buffer);
}

// [state (0 waiting, 1 recording, 2 finished), frames written (after the pre-roll), input peak].
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_recordStatus(JNIEnv* env, jobject, jlong handle) {
    auto* b = reinterpret_cast<ps::RecordBuffer*>(handle);
    const int64_t written = std::max<int64_t>(0, b->written.load(std::memory_order_acquire) - b->preroll);
    const jfloat values[3] = {static_cast<jfloat>(b->state.load(std::memory_order_acquire)),
                              static_cast<jfloat>(written), b->peak.load(std::memory_order_relaxed)};
    jfloatArray out = env->NewFloatArray(3);
    if (out) env->SetFloatArrayRegion(out, 0, 3, values);
    return out;
}

// The engine no longer writes into it: the pre-roll ring is put in order, then copied out (the
// pre-roll first, then the take).
static jfloatArray takeOut(JNIEnv* env, ps::RecordBuffer* b) {
    const auto frames = static_cast<jsize>(std::min<int64_t>(b->written.load(std::memory_order_acquire), static_cast<int64_t>(b->data.size())));
    if (b->preroll > 0 && frames >= b->preroll) {
        const int64_t head = std::clamp<int64_t>(b->prerollHead.load(std::memory_order_acquire), 0, b->preroll);
        std::rotate(b->data.begin(), b->data.begin() + head, b->data.begin() + b->preroll);
        b->prerollHead.store(0, std::memory_order_relaxed);
    }
    jfloatArray out = env->NewFloatArray(frames);
    if (out && frames > 0) env->SetFloatArrayRegion(out, 0, frames, b->data.data());
    return out;
}

// Ends the recording and returns what was recorded (mono, 48 kHz), freeing the buffer. When the
// stream is closed (the app was closed in the middle of a take), the engine cannot let go of it:
// what was recorded is copied out all the same (the audio thread cannot run meanwhile) and the
// buffer is kept until the engine lets go. The same when the engine has not answered yet.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_recordFinish(JNIEnv* env, jobject, jlong handle) {
    auto* b = reinterpret_cast<ps::RecordBuffer*>(handle);
    host().post({ps::CommandType::RecordStop, 0.0f});
    for (int waited = 0; waited < 1000 && b->state.load(std::memory_order_acquire) != 2; waited += 2) {
        if (host().runWhileStopped([] {})) break;  // closed: no callback will apply the stop
        std::this_thread::sleep_for(std::chrono::milliseconds(2));
    }
    host().stopRecordingInput(ps::AudioHost::MicRecording);
    if (b->state.load(std::memory_order_acquire) != 2) {
        // Frames [0, written) never change once published (written is stored with release after
        // them): copied out even while the engine still holds the buffer (a queue full of a resync,
        // Bluetooth callbacks far apart), never a take lost (audit 13).
        jfloatArray out = takeOut(env, b);
        // The engine may still apply the queued commands when the stream comes back: keep the
        // buffer until it says it let go.
        std::lock_guard<std::mutex> guard(zombieLock);
        zombies.push_back(b);
        return out;
    }
    jfloatArray out = takeOut(env, b);
    delete b;
    freeReleasedTakes();
    return out;
}

// Steps since the song (re)started (song automation), -1 when stopped.
JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeSongStep(JNIEnv*, jobject) {
    return static_cast<jint>(host().engine().songStep());
}

// --- Analyser and tuner ---

// The last Engine::kScopeFrames of the output (mono) or of the microphone into `out`.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_readScope(JNIEnv* env, jobject, jboolean microphone, jfloatArray out) {
    if (env->GetArrayLength(out) < ps::Engine::kScopeFrames) return;
    float buf[ps::Engine::kScopeFrames];
    host().engine().readScope(microphone, buf);
    env->SetFloatArrayRegion(out, 0, ps::Engine::kScopeFrames, buf);
}

// « Mode retours »: the output's copy for the screen recording (one reader thread).
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_tapStart(JNIEnv*, jobject) { host().engine().tapStart(); }
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_tapStop(JNIEnv*, jobject) { host().engine().tapStop(); }
JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_tapRead(JNIEnv* env, jobject, jfloatArray out) {
    const jsize capacity = env->GetArrayLength(out);
    float buf[4096];
    jsize done = 0;
    while (done < capacity) {
        const std::size_t want = static_cast<std::size_t>(std::min<jsize>(4096, capacity - done));
        const std::size_t got = host().engine().tapRead(buf, want);
        if (got == 0) break;
        env->SetFloatArrayRegion(out, done, static_cast<jsize>(got), buf);
        done += static_cast<jsize>(got);
        if (got < (want & ~std::size_t{1})) break;
    }
    return done;
}

// Microphone on for the tuner (false: unavailable); off again with micClose.
JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_micOpen(JNIEnv*, jobject) {
    return host().startRecordingInput(ps::AudioHost::MicTuner) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_micClose(JNIEnv*, jobject) {
    host().stopRecordingInput(ps::AudioHost::MicTuner);
}

// --- Live pads ---

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeInstallClip(JNIEnv*, jobject, jint pad, jlong handle) {
    auto& engine = host().engine();
    engine.collectGarbage();
    auto* s = reinterpret_cast<ps::Sample*>(handle);
    if (s && s->compact()) return JNI_FALSE;  // a multizone zone: only MultiSampler reads 16-bit samples
    if (s) s->retain();
    ps::Command command{ps::CommandType::InstallClip, 0.0f, pad, 0};
    command.sample = s;
    if (host().post(command)) return JNI_TRUE;
    if (s && s->release()) delete s;
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_clipStates(JNIEnv* env, jobject, jintArray out) {
    jint states[ps::ClipLauncher::kSlots];
    for (int i = 0; i < ps::ClipLauncher::kSlots; ++i) states[i] = host().engine().clipState(i);
    const jsize n = std::min<jsize>(env->GetArrayLength(out), ps::ClipLauncher::kSlots);
    env->SetIntArrayRegion(out, 0, n, states);
}

// The audio track whose microphone is sent back now (-1: none, also once the output reopened). Any thread.
JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeMonitorTrack(JNIEnv*, jobject) {
    return static_cast<jint>(host().engine().monitorTrack());
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeSceneSwaps(JNIEnv*, jobject) {
    return static_cast<jint>(host().engine().sceneSwaps());
}

// --- Live grid ---

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeTransportStep(JNIEnv*, jobject) {
    return static_cast<jint>(host().engine().transportStep());
}

JNIEXPORT jdouble JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeTransportPosition(JNIEnv*, jobject) {
    return host().engine().transportPosition();
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeSongMarkStep(JNIEnv*, jobject) {
    return static_cast<jint>(host().engine().songMarkStep());
}

// Per slot: tag playing, tag waiting, start step (3 * Engine::kSlots values).
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeSlotStates(JNIEnv* env, jobject, jintArray out) {
    jint values[ps::kSlots * 3];
    auto& engine = host().engine();
    for (int k = 0; k < ps::kSlots; ++k) {
        values[k * 3] = engine.slotTag(k);
        values[k * 3 + 1] = engine.slotPending(k);
        values[k * 3 + 2] = engine.slotStart(k);
    }
    const jsize n = std::min<jsize>(env->GetArrayLength(out), ps::kSlots * 3);
    env->SetIntArrayRegion(out, 0, n, values);
}

// The launches that happened since the last call (slot, tag, step each), at most out.length / 3.
JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeDrainLaunchEvents(JNIEnv* env, jobject, jintArray out) {
    const jsize room = env->GetArrayLength(out) / 3;
    jint values[ps::Engine::kLaunchEvents * 3];
    jsize n = 0;
    ps::LaunchEvent e;
    while (n < room && n < ps::Engine::kLaunchEvents && host().engine().popLaunchEvent(e)) {
        values[n * 3] = e.slot;
        values[n * 3 + 1] = e.tag;
        values[n * 3 + 2] = e.step;
        ++n;
    }
    if (n > 0) env->SetIntArrayRegion(out, 0, n * 3, values);
    return n;
}

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_stemPosition(JNIEnv*, jobject) {
    return static_cast<jlong>(host().engine().stemPosition());
}

// --- General MIDI instruments (SoundFont). Load on a worker thread, the rest on the main thread. ---

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_soundFontLoad(JNIEnv* env, jobject, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    if (!p) return 0;
    tsf* f = ps::SoundFontPlayer::loadFile(p);
    env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(f);
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_soundFontClose(JNIEnv*, jobject, jlong handle) {
    ps::SoundFontPlayer::close(reinterpret_cast<tsf*>(handle));
}

// "bank|program|name" for each preset index.
JNIEXPORT jobjectArray JNICALL Java_com_pocketstudio_engine_NativeEngine_soundFontPresets(JNIEnv* env, jobject, jlong handle) {
    auto* f = reinterpret_cast<tsf*>(handle);
    const int n = ps::SoundFontPlayer::presetCount(f);
    jobjectArray out = env->NewObjectArray(n, env->FindClass("java/lang/String"), nullptr);
    for (int i = 0; out && i < n; ++i) {
        int bank = 0, program = 0;
        const char* name = "";
        ps::SoundFontPlayer::presetInfo(f, i, &bank, &program, &name);
        char line[64];
        std::snprintf(line, sizeof line, "%d|%d|%.40s", bank, program, name);
        jstring s = env->NewStringUTF(line);
        env->SetObjectArrayElement(out, i, s);
        env->DeleteLocalRef(s);
    }
    return out;
}

// Main thread (shares the font's reference count). Mono at 48 kHz, trailing silence trimmed.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_soundFontRenderNote(JNIEnv* env, jobject, jlong handle,
                                                                                          jint preset, jint note,
                                                                                          jfloat velocity, jint holdFrames,
                                                                                          jint frames) {
    if (!handle || frames <= 0 || frames > 48000 * 10) return env->NewFloatArray(0);
    std::vector<float> out(static_cast<size_t>(frames));
    if (!ps::SoundFontPlayer::renderNote(reinterpret_cast<tsf*>(handle), preset, note, velocity, holdFrames, out.data(), frames,
                                         48000)) {
        return env->NewFloatArray(0);
    }
    size_t end = out.size();
    while (end > 480 && std::fabs(out[end - 1]) < 1e-4f) --end;
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(end));
    if (result) env->SetFloatArrayRegion(result, 0, static_cast<jsize>(end), out.data());
    return result;
}

static ps::OfflineRenderer* renderer(jlong handle);

static ps::Command instrumentCommand(jlong font, jint preset) {
    ps::Command c{ps::CommandType::SetInstrument, 0.0f, preset, 0};
    c.font = font ? ps::SoundFontPlayer::makeInstance(reinterpret_cast<tsf*>(font), 48000) : nullptr;
    return c;
}

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeSetInstrument(JNIEnv*, jobject, jlong font, jint preset) {
    host().engine().collectGarbage();
    ps::Command c = instrumentCommand(font, preset);
    if (font && !c.font) return JNI_FALSE;
    if (host().post(c)) return JNI_TRUE;
    ps::SoundFontPlayer::close(c.font);
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportSetInstrument(JNIEnv*, jobject, jlong exportHandle, jlong font,
                                                                                   jint preset) {
    renderer(exportHandle)->post(instrumentCommand(font, preset));
}

// --- Offline export: a private engine rendered on the caller's (worker) thread. ---

static ps::OfflineRenderer* renderer(jlong handle) { return reinterpret_cast<ps::OfflineRenderer*>(handle); }

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_exportCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new ps::OfflineRenderer(48000));
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportPost(JNIEnv*, jobject, jlong handle, jint type,
                                                                          jfloat value, jint a, jint b) {
    renderer(handle)->post({static_cast<ps::CommandType>(type), value, a, b});
}

// Later posts to this export take effect at `frame` of the song (-1: at once). Song exports.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportScheduleFrom(JNIEnv*, jobject, jlong handle, jlong frame) {
    renderer(handle)->scheduleFrom(frame);
}

// The last `frames` of the next render fade to silence (0: none).
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportSetFadeOut(JNIEnv*, jobject, jlong handle, jlong frames) {
    renderer(handle)->setFadeOut(frames);
}

// The next render's tail ends by itself (tail = a cap); its WAV is 32-bit float.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportSetOptions(JNIEnv*, jobject, jlong handle, jboolean autoTail,
                                                                                jboolean floatWav) {
    renderer(handle)->setAutoTail(autoTail);
    renderer(handle)->setFloatWav(floatWav);
}

// The next WAV's format: 44100 or 48000 Hz, 16 or 24 bits (dithered) or 32 (float).
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportSetFormat(JNIEnv*, jobject, jlong handle, jint rate, jint bits) {
    renderer(handle)->setOutputFormat(rate, bits);
}

// What the last render wrote (layout: NativeEngine.REPORT_*): integrated loudness (LUFS, -1000
// for silence), true peak (dBTP), length (seconds), sample rate, the loudness target's drive (dB).
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_exportReport(JNIEnv* env, jobject, jlong handle) {
    const auto& r = renderer(handle)->report();
    // S5: then what the clipper and the limiter took off (LU), the loud part's loudness (LUFS, the
    // Drop; -1000 without a two-pass Drop render) and the target's mode (0 song, 1 Drop).
    const jfloat values[8] = {
        static_cast<jfloat>(r.lufs),
        r.truePeak > 0.0f ? 20.0f * std::log10(r.truePeak) : -1000.0f,
        r.sampleRate > 0 ? static_cast<jfloat>(static_cast<double>(r.frames) / r.sampleRate) : 0.0f,
        static_cast<jfloat>(r.sampleRate),
        r.driveDb,
        r.lossDb,
        static_cast<jfloat>(r.loudLufs),
        static_cast<jfloat>(r.mode),
    };
    jfloatArray result = env->NewFloatArray(8);
    if (result) env->SetFloatArrayRegion(result, 0, 8, values);
    return result;
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_exportRender(JNIEnv* env, jobject, jlong handle,
                                                                            jstring path, jlong frames, jlong tail) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    if (!p) return static_cast<jint>(ps::OfflineRenderer::Result::IoError);
    const auto result = renderer(handle)->renderToWav(p, frames, tail);
    env->ReleaseStringUTFChars(path, p);
    return static_cast<jint>(result);
}

// The whole song into memory (interleaved stereo, 48 kHz), for resampling onto a pad.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_exportRenderFloat(JNIEnv* env, jobject, jlong handle,
                                                                                        jlong frames, jlong tail) {
    const auto out = renderer(handle)->renderToBuffer(frames, tail);
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(out.size()));
    if (result && !out.empty()) env->SetFloatArrayRegion(result, 0, static_cast<jsize>(out.size()), out.data());
    return result;
}

// S7: a render for its report only (the live gain's estimate): no samples handed to Kotlin.
JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_exportRenderReport(JNIEnv*, jobject, jlong handle,
                                                                                  jlong frames, jlong tail) {
    return static_cast<jint>(renderer(handle)->renderForReport(frames, tail));
}

// The same in mono (the mean of both channels): a long frozen track, half the memory.
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_exportRenderFloatMono(JNIEnv* env, jobject, jlong handle,
                                                                                            jlong frames, jlong tail) {
    auto out = renderer(handle)->renderToBuffer(frames, tail, true);
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(out.size()));
    if (result && !out.empty()) env->SetFloatArrayRegion(result, 0, static_cast<jsize>(out.size()), out.data());
    return result;
}

JNIEXPORT jfloat JNICALL Java_com_pocketstudio_engine_NativeEngine_exportProgress(JNIEnv*, jobject, jlong handle) {
    return renderer(handle)->progress();
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportCancel(JNIEnv*, jobject, jlong handle) {
    renderer(handle)->cancel();
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportDestroy(JNIEnv*, jobject, jlong handle) {
    delete renderer(handle);
}

// Fills `out` (NativeEngine.METERS floats): each channel's peak (index kMaster: the master's
// output), the limiter's lowest gain since the last reading, then the master's peak going into
// the limiter. Layout must match NativeEngine.METER_LIMITER / METER_MASTER_IN.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_mixerMeters(JNIEnv* env, jobject, jfloatArray out) {
    auto& m = host().engine().mixer();
    constexpr int kMeters = ps::Mixer::kProjectChannels + 2;
    jfloat values[kMeters];
    for (int ch = 0; ch < ps::Mixer::kProjectChannels; ++ch) values[ch] = ch == ps::Mixer::kMaster ? m.takeMasterPeak() : m.takeChannelPeak(ch);
    values[ps::Mixer::kProjectChannels] = m.takeLimiterGain();
    values[ps::Mixer::kProjectChannels + 1] = m.takeMasterInputPeak();
    const int n = std::min(kMeters, static_cast<int>(env->GetArrayLength(out)));
    if (n > 0) env->SetFloatArrayRegion(out, 0, n, values);
}

// Fills `out` (NativeEngine.INSERT_METERS floats): for each strip and insert slot (index
// channel * 8 + slot), the deepest gain reduction of its compressor since the last reading, in dB (<= 0).
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_insertReductions(JNIEnv* env, jobject, jfloatArray out) {
    auto& m = host().engine().mixer();
    constexpr int kMeters = ps::Mixer::kProjectChannels * ps::Mixer::kInserts;
    jfloat values[kMeters];
    for (int i = 0; i < kMeters; ++i) values[i] = m.takeInsertReduction(i / ps::Mixer::kInserts, i % ps::Mixer::kInserts);
    const int n = std::min(kMeters, static_cast<int>(env->GetArrayLength(out)));
    if (n > 0) env->SetFloatArrayRegion(out, 0, n, values);
}

// Fills `out` (NativeEngine.LOUDNESS floats): the master's momentary, short-term and integrated
// (since ▶) loudness in LUFS (-100: none yet), its true peak since ▶ and since the last reading
// (dBTP, -100 for silence), the loudness target's drive (dB). Layout: NativeEngine.LOUDNESS_*.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_loudnessMeters(JNIEnv* env, jobject, jfloatArray out) {
    auto& m = host().engine().mixer();
    auto& meter = m.loudness();
    auto db = [](float peak) { return peak > 1e-5f ? 20.0f * std::log10(peak) : -100.0f; };
    // S5: then what the clipper and the limiter take off (LU), as the target's estimate reads it.
    jfloat values[7] = {meter.momentary(), meter.shortTerm(), meter.integrated(), db(meter.truePeak()), db(meter.takeRecentPeak()),
                        m.masterChain().driveDb(), m.masterChain().lossDb()};
    const int n = std::min(7, static_cast<int>(env->GetArrayLength(out)));
    if (n > 0) env->SetFloatArrayRegion(out, 0, n, values);
}

// Fills `out` (NativeEngine.HEALTH floats) for the bug report's health journal, without taking
// anything another meter reads: the master's momentary and short-term loudness (LUFS), the
// limiter's gain now (linear) and the audio thread's load. Atomics only, from the UI thread.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_healthMeters(JNIEnv* env, jobject, jfloatArray out) {
    auto& e = host().engine();
    auto& m = e.mixer();
    jfloat values[4] = {m.loudness().momentary(), m.loudness().shortTerm(), m.limiterGain(), e.cpuLoad()};
    const int n = std::min(4, static_cast<int>(env->GetArrayLength(out)));
    if (n > 0) env->SetFloatArrayRegion(out, 0, n, values);
}

// Fills `out` (16 ints) with how many times each drum lane has fired, for pad lights.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_laneTriggers(JNIEnv* env, jobject, jintArray out) {
    jint values[ps::Engine::kLanes];
    for (int i = 0; i < ps::Engine::kLanes; ++i) values[i] = static_cast<jint>(host().engine().laneTriggerCount(i));
    env->SetIntArrayRegion(out, 0, ps::Engine::kLanes, values);
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
        static_cast<double>(info.restarts),
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
    return env->NewStringUTF(host().inputDescription().c_str());
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_latencyTestState(JNIEnv*, jobject) {
    return static_cast<jint>(host().engine().latencyMeter().state());
}

// Returns [valid, latencyMs, confidence, inputPeak, inputFrames, failure] and closes the input stream.
JNIEXPORT jdoubleArray JNICALL Java_com_pocketstudio_engine_NativeEngine_finishLatencyTest(JNIEnv* env, jobject) {
    auto& h = host();
    ps::RoundTripMeter::Result result;
    const bool done = h.engine().latencyMeter().state() == ps::RoundTripMeter::State::Done;
    if (done) result = h.engine().latencyMeter().analyze();
    if (!done && h.latencyTestInterrupted()) result.failure = ps::RoundTripMeter::Failure::Interrupted;
    // A cancelled run says nothing about the microphone configuration.
    h.finishLatencyTest(!done || result.inputFrames > 0);
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
