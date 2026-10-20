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

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativePost(JNIEnv*, jobject, jint type,
                                                                          jfloat value, jint a, jint b) {
    return host().post({static_cast<ps::CommandType>(type), value, a, b}) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_currentStep(JNIEnv*, jobject) {
    return host().engine().currentStep();
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_setBufferBursts(JNIEnv*, jobject, jint bursts) {
    host().setBufferBursts(bursts);
}

JNIEXPORT jdouble JNICALL Java_com_pocketstudio_engine_NativeEngine_playPosition(JNIEnv*, jobject) {
    return host().engine().playPosition();
}

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_absoluteStep(JNIEnv*, jobject) {
    return host().engine().absoluteStep();
}

// --- Samples: created and released off the audio thread, shared by reference. ---

JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_sampleCreate(JNIEnv* env, jobject, jfloatArray data,
                                                                             jint channels, jint sampleRate) {
    const jsize length = env->GetArrayLength(data);
    if (channels < 1 || length < channels) return 0;
    jfloat* in = env->GetFloatArrayElements(data, nullptr);
    if (!in) return 0;
    ps::Sample* s = ps::Sample::create(in, length / channels, channels, sampleRate, 48000);
    env->ReleaseFloatArrayElements(data, in, JNI_ABORT);
    return reinterpret_cast<jlong>(s);
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
JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_sampleStretched(JNIEnv*, jobject, jlong handle, jfloat start,
                                                                                jfloat end, jlong outFrames, jfloat semitones) {
    auto* s = reinterpret_cast<ps::Sample*>(handle);
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
    if (s) s->retain();
    ps::Command command{ps::CommandType::InstallAudioClip, 0.0f, track, 0};
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
// `onStart`: the recording begins exactly when the transport reaches its first step.
JNIEXPORT jlong JNICALL Java_com_pocketstudio_engine_NativeEngine_recordStart(JNIEnv*, jobject, jboolean onStart, jint seconds) {
    if (seconds <= 0 || seconds > 600) return 0;
    freeReleasedTakes();
    if (!host().startRecordingInput()) return 0;
    auto* buffer = new ps::RecordBuffer(static_cast<int64_t>(seconds) * 48000);
    ps::Command command{ps::CommandType::RecordArm, 0.0f, onStart ? 1 : 0, 0};
    command.record = buffer;
    if (!host().post(command)) {
        delete buffer;
        host().stopRecordingInput();
        return 0;
    }
    return reinterpret_cast<jlong>(buffer);
}

// [state (0 waiting, 1 recording, 2 finished), frames written, input peak].
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_recordStatus(JNIEnv* env, jobject, jlong handle) {
    auto* b = reinterpret_cast<ps::RecordBuffer*>(handle);
    const jfloat values[3] = {static_cast<jfloat>(b->state.load(std::memory_order_acquire)),
                              static_cast<jfloat>(b->written.load(std::memory_order_acquire)), b->peak.load(std::memory_order_relaxed)};
    jfloatArray out = env->NewFloatArray(3);
    if (out) env->SetFloatArrayRegion(out, 0, 3, values);
    return out;
}

// Ends the recording and returns what was recorded (mono, 48 kHz), freeing the buffer; null when
// the engine could not let go of it in time (the buffer is then kept, never freed under it).
JNIEXPORT jfloatArray JNICALL Java_com_pocketstudio_engine_NativeEngine_recordFinish(JNIEnv* env, jobject, jlong handle) {
    auto* b = reinterpret_cast<ps::RecordBuffer*>(handle);
    host().post({ps::CommandType::RecordStop, 0.0f});
    for (int waited = 0; waited < 1000 && b->state.load(std::memory_order_acquire) != 2; waited += 2) {
        std::this_thread::sleep_for(std::chrono::milliseconds(2));
    }
    host().stopRecordingInput();
    if (b->state.load(std::memory_order_acquire) != 2) {
        // The engine may still apply the queued commands when the stream comes back: keep the
        // buffer until it says it let go.
        std::lock_guard<std::mutex> guard(zombieLock);
        zombies.push_back(b);
        return nullptr;
    }
    const auto frames = static_cast<jsize>(std::min<int64_t>(b->written.load(std::memory_order_acquire), static_cast<int64_t>(b->data.size())));
    jfloatArray out = env->NewFloatArray(frames);
    if (out && frames > 0) env->SetFloatArrayRegion(out, 0, frames, b->data.data());
    delete b;
    freeReleasedTakes();
    return out;
}

// --- Analyser and tuner ---

// The last Engine::kScopeFrames of the output (mono) or of the microphone into `out`.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_readScope(JNIEnv* env, jobject, jboolean microphone, jfloatArray out) {
    if (env->GetArrayLength(out) < ps::Engine::kScopeFrames) return;
    float buf[ps::Engine::kScopeFrames];
    host().engine().readScope(microphone, buf);
    env->SetFloatArrayRegion(out, 0, ps::Engine::kScopeFrames, buf);
}

// Microphone on for the tuner (false: unavailable); off again with micClose.
JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_micOpen(JNIEnv*, jobject) {
    return host().startRecordingInput() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_micClose(JNIEnv*, jobject) {
    host().stopRecordingInput();
}

// --- Live pads ---

JNIEXPORT jboolean JNICALL Java_com_pocketstudio_engine_NativeEngine_nativeInstallClip(JNIEnv*, jobject, jint pad, jlong handle) {
    auto& engine = host().engine();
    engine.collectGarbage();
    auto* s = reinterpret_cast<ps::Sample*>(handle);
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

JNIEXPORT jint JNICALL Java_com_pocketstudio_engine_NativeEngine_sceneSwaps(JNIEnv*, jobject) {
    return static_cast<jint>(host().engine().sceneSwaps());
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

JNIEXPORT jfloat JNICALL Java_com_pocketstudio_engine_NativeEngine_exportProgress(JNIEnv*, jobject, jlong handle) {
    return renderer(handle)->progress();
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportCancel(JNIEnv*, jobject, jlong handle) {
    renderer(handle)->cancel();
}

JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_exportDestroy(JNIEnv*, jobject, jlong handle) {
    delete renderer(handle);
}

// Fills `out` (5 floats): channel peaks (drums, acid, synth), master peak, limiter gain.
JNIEXPORT void JNICALL Java_com_pocketstudio_engine_NativeEngine_mixerMeters(JNIEnv* env, jobject, jfloatArray out) {
    auto& m = host().engine().mixer();
    // Channel peaks (index kMaster: the master), then the limiter gain.
    jfloat values[ps::Mixer::kChannels + 1];
    for (int ch = 0; ch < ps::Mixer::kChannels; ++ch) values[ch] = ch == ps::Mixer::kMaster ? m.takeMasterPeak() : m.takeChannelPeak(ch);
    values[ps::Mixer::kChannels] = m.limiterGain();
    if (env->GetArrayLength(out) >= ps::Mixer::kChannels + 1) env->SetFloatArrayRegion(out, 0, ps::Mixer::kChannels + 1, values);
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
