#include "AudioHost.h"

#include <android/log.h>

#include <algorithm>
#include <cstdio>
#include <chrono>
#include <thread>

#define LOG_TAG "PocketStudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace ps {

namespace {
constexpr int kSampleRate = 48000;
constexpr int kOutputChannels = 2;
constexpr int kMaxInputFrames = 8192;
constexpr std::size_t kInputRingSize = 1 << 16;  // ~1.4 s at 48 kHz

// Microphone + speaker combinations tried in order for the latency test. On some phones
// (e.g. Honor/MagicOS) starting the microphone reroutes audio and cuts an exclusive
// output stream, so shared modes come early.
struct LatencyConfig {
    oboe::InputPreset preset;
    oboe::SharingMode inputSharing;
    oboe::SharingMode outputSharing;
    const char* description;
};

constexpr LatencyConfig kLatencyConfigs[] = {
    {oboe::InputPreset::Unprocessed, oboe::SharingMode::Exclusive, oboe::SharingMode::Exclusive, "brut, exclusif"},
    {oboe::InputPreset::Unprocessed, oboe::SharingMode::Shared, oboe::SharingMode::Shared, "brut, partagé"},
    {oboe::InputPreset::VoiceRecognition, oboe::SharingMode::Shared, oboe::SharingMode::Shared, "reconnaissance vocale, partagé"},
    {oboe::InputPreset::Generic, oboe::SharingMode::Shared, oboe::SharingMode::Shared, "standard, partagé"},
    {oboe::InputPreset::VoiceRecognition, oboe::SharingMode::Exclusive, oboe::SharingMode::Exclusive, "reconnaissance vocale, exclusif"},
};
constexpr int kLatencyConfigCount = static_cast<int>(sizeof(kLatencyConfigs) / sizeof(kLatencyConfigs[0]));
constexpr int kMicWaitMs = 1000;
}  // namespace

AudioHost::AudioHost()
    : engine_(kSampleRate), inputBuffer_(kMaxInputFrames, 0.0f), inputRing_(kInputRingSize), inputCallback_(inputRing_) {}

AudioHost::~AudioHost() { stop(); }

bool AudioHost::openOutputLocked(oboe::SharingMode sharing) {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(sharing)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(kOutputChannels)
        ->setSampleRate(kSampleRate)
        ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
        ->setUsage(oboe::Usage::Media)
        ->setContentType(oboe::ContentType::Music)
        ->setDataCallback(this)
        ->setErrorCallback(this);
    const auto result = builder.openStream(output_);
    if (result != oboe::Result::OK) {
        LOGW("openStream(output) failed: %s", oboe::convertToText(result));
        output_.reset();
        return false;
    }
    // Two bursts is the usual sweet spot between latency and glitch safety (user setting).
    output_->setBufferSizeInFrames(output_->getFramesPerBurst() * bufferBursts_);
    output_->setPerformanceHintEnabled(true);
    LOGI("output: rate=%d burst=%d api=%s sharing=%s", output_->getSampleRate(),
         output_->getFramesPerBurst(), output_->getAudioApi() == oboe::AudioApi::AAudio ? "AAudio" : "OpenSL",
         output_->getSharingMode() == oboe::SharingMode::Exclusive ? "exclusive" : "shared");
    return true;
}

void AudioHost::describeLocked(const char* status) {
    inputDescription_ = std::string(kLatencyConfigs[config_].description) + " (" + status + ")";
}

void AudioHost::advanceConfigLocked() { config_ = (config_ + 1) % kLatencyConfigCount; }

bool AudioHost::openInputLocked() {
    const auto& config = kLatencyConfigs[config_];
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(config.inputSharing)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(1)
        ->setSampleRate(kSampleRate)
        ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
        ->setInputPreset(config.preset)
        ->setDataCallback(&inputCallback_)
        ->setErrorCallback(this);
    const auto result = builder.openStream(input_);
    if (result != oboe::Result::OK) {
        LOGW("openStream(input, %s) failed: %s", config.description, oboe::convertToText(result));
        input_.reset();
        return false;
    }
    LOGI("input: %s, rate=%d channels=%d", config.description, input_->getSampleRate(), input_->getChannelCount());
    return true;
}

void AudioHost::closeLocked() {
    inputLive_.store(false, std::memory_order_release);
    if (output_) {
        auto xruns = output_->getXRunCount();
        if (xruns) xrunBase_ += xruns.value();
        lastXruns_ = xrunBase_;
        output_->stop();
        output_->close();
        output_.reset();
    }
    if (input_) {
        input_->stop();
        input_->close();
        input_.reset();
    }
}

void AudioHost::restoreOutputLocked() {
    if (!wantRunning_ || !openOutputLocked()) return;
    engine_.prepare(output_->getSampleRate());
    output_->requestStart();
}

bool AudioHost::start() {
    std::lock_guard<std::mutex> guard(lock_);
    wantRunning_ = true;
    if (output_ && output_->getState() == oboe::StreamState::Started) return true;
    closeLocked();
    if (!openOutputLocked()) return false;
    engine_.prepare(output_->getSampleRate());
    if (output_->requestStart() != oboe::Result::OK) {
        closeLocked();
        return false;
    }
    return true;
}

void AudioHost::stop() {
    std::lock_guard<std::mutex> guard(lock_);
    wantRunning_ = false;
    closeLocked();
}

void AudioHost::setBufferBursts(int bursts) {
    std::lock_guard<std::mutex> guard(lock_);
    bufferBursts_ = bursts < 1 ? 1 : (bursts > 8 ? 8 : bursts);
    if (output_) output_->setBufferSizeInFrames(output_->getFramesPerBurst() * bufferBursts_);
}

bool AudioHost::post(const Command& command) {
    std::lock_guard<std::mutex> guard(postLock_);
    return engine_.post(command);
}

AudioHost::LatencyStart AudioHost::startLatencyTest() {
    std::lock_guard<std::mutex> guard(lock_);
    // The app went to the background meanwhile: never open the mic behind the user's back.
    if (!wantRunning_) return LatencyStart::NotForeground;
    closeLocked();
    testInterrupted_.store(false, std::memory_order_release);
    const auto& config = kLatencyConfigs[config_];

    // 1. Speaker first, playing silence.
    if (!openOutputLocked(config.outputSharing)) {
        describeLocked("haut-parleur refusé");
        advanceConfigLocked();
        restoreOutputLocked();
        return LatencyStart::OutputFailed;
    }
    engine_.prepare(output_->getSampleRate());
    if (output_->requestStart() != oboe::Result::OK) {
        describeLocked("haut-parleur bloqué");
        closeLocked();
        advanceConfigLocked();
        restoreOutputLocked();
        return LatencyStart::OutputFailed;
    }

    // 2. Then the microphone.
    inputCallback_.reset();
    if (!openInputLocked() || input_->requestStart() != oboe::Result::OK) {
        describeLocked("micro refusé");
        closeLocked();
        advanceConfigLocked();
        restoreOutputLocked();
        return LatencyStart::MicUnavailable;
    }

    // 3. Wait until the microphone really delivers (some phones need a moment, or reroute).
    for (int waited = 0; waited < kMicWaitMs && inputCallback_.frames() < kSampleRate / 20; waited += 10) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    const bool streamsAlive = output_ && input_ && output_->getState() == oboe::StreamState::Started &&
                              input_->getState() == oboe::StreamState::Started;
    if (inputCallback_.frames() < kSampleRate / 20 || !streamsAlive) {
        char status[96];
        snprintf(status, sizeof(status), streamsAlive ? "micro muet : %lld échantillons en 1 s"
                                                      : "flux coupé au démarrage du micro (%lld échantillons)",
                 static_cast<long long>(inputCallback_.frames()));
        describeLocked(status);
        closeLocked();
        advanceConfigLocked();
        restoreOutputLocked();
        return LatencyStart::MicSilent;
    }

    // 4. Start: the next output callback drops stale input and starts the meter together.
    drainInput_.store(true, std::memory_order_release);
    inputLive_.store(true, std::memory_order_release);
    {
        std::lock_guard<std::mutex> postGuard(postLock_);
        engine_.post({CommandType::StartLatencyTest, 0.0f});
    }
    // The audio thread picks the command up on its next callback: only report "started"
    // once the meter really runs, so callers never mistake "not started yet" for "finished".
    for (int waited = 0; waited < 500; waited += 2) {
        if (engine_.latencyMeter().state() != RoundTripMeter::State::Idle) break;
        std::this_thread::sleep_for(std::chrono::milliseconds(2));
    }
    if (engine_.latencyMeter().state() == RoundTripMeter::State::Idle) {
        describeLocked("le moteur n'a pas lancé la mesure");
        closeLocked();
        restoreOutputLocked();
        return LatencyStart::OutputFailed;
    }
    describeLocked("en cours");
    return LatencyStart::Ok;
}

void AudioHost::finishLatencyTest(bool succeededInput) {
    std::lock_guard<std::mutex> guard(lock_);
    const bool interrupted = testInterrupted_.load();
    // Nothing running (the start already failed and reported why): keep the normal stream.
    if (!input_ && output_ && !interrupted) return;
    char status[128];
    snprintf(status, sizeof(status), "%lld échantillons reçus%s",
             static_cast<long long>(inputCallback_.frames()), interrupted ? ", flux coupé en route" : "");
    describeLocked(status);
    if (!succeededInput || interrupted) advanceConfigLocked();
    closeLocked();
    // Streams are closed: no audio thread touches the meter now.
    engine_.latencyMeter().cancel();
    restoreOutputLocked();
}

oboe::DataCallbackResult AudioHost::onAudioReady(oboe::AudioStream* stream, void* audioData,
                                                 int32_t numFrames) {
    auto* out = static_cast<float*>(audioData);
    const float* in = nullptr;
    int inFrames = 0;
    if (inputLive_.load(std::memory_order_acquire)) {
        // Drop samples captured before the measurement so both directions start aligned.
        if (drainInput_.exchange(false, std::memory_order_acq_rel)) inputRing_.discard();
        // Take everything the microphone thread has delivered so far. The meter records it
        // back to back, so uneven chunks don't distort the timing.
        inFrames = static_cast<int>(inputRing_.read(inputBuffer_.data(), kMaxInputFrames));
        in = inputBuffer_.data();
    }
    engine_.process(in, inFrames, 1, out, numFrames, stream->getChannelCount());
    return oboe::DataCallbackResult::Continue;
}

void AudioHost::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    // Called on an Oboe-owned thread, e.g. when headphones are plugged in or unplugged,
    // or when starting the microphone reroutes audio.
    LOGW("stream closed: %s", oboe::convertToText(error));
    std::lock_guard<std::mutex> guard(lock_);
    if (stream != output_.get() && stream != input_.get()) return;
    if (input_ && !recordingInput_) {
        // A latency test was running: report it instead of failing silently.
        testInterrupted_.store(true, std::memory_order_release);
    }
    if (stream == output_.get()) {
        xrunBase_ = lastXruns_;
        output_.reset();
    } else {
        input_.reset();
    }
    closeLocked();
    engine_.latencyMeter().cancel();
    // A freshly (un)plugged device is often not ready yet: retry a few times.
    for (int attempt = 0; attempt < 5 && wantRunning_; ++attempt) {
        if (attempt > 0) std::this_thread::sleep_for(std::chrono::milliseconds(200));
        if (!openOutputLocked()) continue;
        engine_.prepare(output_->getSampleRate());
        if (output_->requestStart() == oboe::Result::OK) {
            // Opening the microphone rerouted the audio (some phones): a take keeps its microphone.
            if (recordingInput_) reopenRecordingInputLocked();
            return;
        }
        closeLocked();
    }
}

bool AudioHost::reopenRecordingInputLocked() {
    inputCallback_.reset();
    if (!openInputLocked() || input_->requestStart() != oboe::Result::OK) {
        if (input_) {
            input_->close();
            input_.reset();
        }
        return false;
    }
    // No waiting here (error-callback thread): the ring fills as soon as the microphone delivers.
    drainInput_.store(true, std::memory_order_release);
    inputLive_.store(true, std::memory_order_release);
    return true;
}

bool AudioHost::startRecordingInput() {
    std::lock_guard<std::mutex> guard(lock_);
    if (!wantRunning_ || !output_) return false;
    if (input_) return true;  // already listening
    recordingInput_ = true;
    inputCallback_.reset();
    if (!openInputLocked() || input_->requestStart() != oboe::Result::OK) {
        if (input_) {
            input_->close();
            input_.reset();
        }
        recordingInput_ = false;
        return false;
    }
    for (int waited = 0; waited < kMicWaitMs && inputCallback_.frames() < kSampleRate / 20; waited += 10) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    // A reroute may have reopened everything meanwhile (error callback, after this lock is free):
    // only a silent microphone that is still ours counts as a failure.
    if (input_ && inputCallback_.frames() < kSampleRate / 20) {
        input_->stop();
        input_->close();
        input_.reset();
        recordingInput_ = false;
        return false;
    }
    drainInput_.store(true, std::memory_order_release);
    inputLive_.store(true, std::memory_order_release);
    return true;
}

void AudioHost::stopRecordingInput() {
    std::lock_guard<std::mutex> guard(lock_);
    recordingInput_ = false;
    inputLive_.store(false, std::memory_order_release);
    if (!input_) return;
    input_->stop();
    input_->close();
    input_.reset();
}

std::string AudioHost::inputDescription() {
    std::lock_guard<std::mutex> guard(lock_);
    return inputDescription_;
}

StreamInfo AudioHost::info() {
    // Called from the UI thread: never wait for a stream being opened or closed.
    std::unique_lock<std::mutex> guard(lock_, std::try_to_lock);
    if (!guard.owns_lock()) return lastInfo_;
    StreamInfo info;
    if (!output_) {
        info.xruns = lastXruns_;
        lastInfo_ = info;
        return info;
    }
    info.running = output_->getState() == oboe::StreamState::Started;
    info.sampleRate = output_->getSampleRate();
    info.framesPerBurst = output_->getFramesPerBurst();
    info.bufferSize = output_->getBufferSizeInFrames();
    info.bufferCapacity = output_->getBufferCapacityInFrames();
    info.exclusive = output_->getSharingMode() == oboe::SharingMode::Exclusive;
    info.aaudio = output_->getAudioApi() == oboe::AudioApi::AAudio;
    info.mmap = oboe::OboeExtensions::isMMapUsed(output_.get());
    info.lowLatency = output_->getPerformanceMode() == oboe::PerformanceMode::LowLatency;
    auto xruns = output_->getXRunCount();
    if (xruns) lastXruns_ = xrunBase_ + xruns.value();
    info.xruns = lastXruns_;
    auto latency = output_->calculateLatencyMillis();
    info.outputLatencyMs = latency ? latency.value() : -1.0;
    lastInfo_ = info;
    return info;
}

}  // namespace ps
