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

struct InputConfig {
    oboe::InputPreset preset;
    oboe::SharingMode sharing;
    const char* description;
};

// Tried in order: some phones refuse the raw microphone or exclusive mode.
constexpr InputConfig kInputConfigs[] = {
    {oboe::InputPreset::Unprocessed, oboe::SharingMode::Exclusive, "brut, exclusif"},
    {oboe::InputPreset::VoiceRecognition, oboe::SharingMode::Exclusive, "reconnaissance vocale, exclusif"},
    {oboe::InputPreset::Unprocessed, oboe::SharingMode::Shared, "brut, partagé"},
    {oboe::InputPreset::VoiceRecognition, oboe::SharingMode::Shared, "reconnaissance vocale, partagé"},
    {oboe::InputPreset::Generic, oboe::SharingMode::Shared, "standard, partagé"},
};
}  // namespace

AudioHost::AudioHost()
    : engine_(kSampleRate), inputBuffer_(kMaxInputFrames, 0.0f), inputRing_(kInputRingSize), inputCallback_(inputRing_) {}

AudioHost::~AudioHost() { stop(); }

bool AudioHost::openOutputLocked() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
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
    // Two bursts is the usual sweet spot between latency and glitch safety.
    output_->setBufferSizeInFrames(output_->getFramesPerBurst() * 2);
    output_->setPerformanceHintEnabled(true);
    LOGI("output: rate=%d burst=%d api=%s sharing=%s", output_->getSampleRate(),
         output_->getFramesPerBurst(), output_->getAudioApi() == oboe::AudioApi::AAudio ? "AAudio" : "OpenSL",
         output_->getSharingMode() == oboe::SharingMode::Exclusive ? "exclusive" : "shared");
    return true;
}

void AudioHost::describeInputLocked(const char* config, const char* status) {
    inputDescription_ = std::string(config) + " (" + status + ")";
}

bool AudioHost::openInputLocked() {
    constexpr int count = static_cast<int>(sizeof(kInputConfigs) / sizeof(kInputConfigs[0]));
    for (int attempt = 0; attempt < count; ++attempt) {
        const int index = (nextInputConfig_ + attempt) % count;
        const auto& config = kInputConfigs[index];
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(config.sharing)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(1)
            ->setSampleRate(kSampleRate)
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
            ->setInputPreset(config.preset)
            ->setDataCallback(&inputCallback_);
        const auto result = builder.openStream(input_);
        if (result != oboe::Result::OK) {
            LOGW("openStream(input, %s) failed: %s", config.description, oboe::convertToText(result));
            describeInputLocked(config.description, "ouverture refusée");
            input_.reset();
            continue;
        }
        currentInputConfig_ = index;
        describeInputLocked(config.description, "ouvert");
        LOGI("input: %s, rate=%d channels=%d", config.description, input_->getSampleRate(),
             input_->getChannelCount());
        return true;
    }
    return false;
}

void AudioHost::closeLocked() {
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

bool AudioHost::post(const Command& command) {
    std::lock_guard<std::mutex> guard(postLock_);
    return engine_.post(command);
}

AudioHost::LatencyStart AudioHost::startLatencyTest() {
    std::lock_guard<std::mutex> guard(lock_);
    // The app went to the background meanwhile: never open the mic behind the user's back.
    if (!wantRunning_) return LatencyStart::NotForeground;
    closeLocked();
    const bool inputOk = openInputLocked();
    if (!inputOk || !openOutputLocked()) {
        closeLocked();
        if (openOutputLocked()) {
            engine_.prepare(output_->getSampleRate());
            output_->requestStart();
        }
        return inputOk ? LatencyStart::OutputFailed : LatencyStart::MicUnavailable;
    }
    engine_.prepare(output_->getSampleRate());
    inputCallback_.reset();
    if (input_->requestStart() != oboe::Result::OK) {
        describeInputLocked(kInputConfigs[currentInputConfig_].description, "démarrage refusé");
        nextInputConfig_ = currentInputConfig_ + 1;
        closeLocked();
        if (openOutputLocked()) {
            engine_.prepare(output_->getSampleRate());
            output_->requestStart();
        }
        return LatencyStart::MicUnavailable;
    }
    drainInput_.store(true, std::memory_order_release);
    {
        std::lock_guard<std::mutex> postGuard(postLock_);
        engine_.post({CommandType::StartLatencyTest, 0.0f});
    }
    if (output_->requestStart() != oboe::Result::OK) {
        closeLocked();
        engine_.latencyMeter().cancel();
        if (openOutputLocked()) {
            engine_.prepare(output_->getSampleRate());
            output_->requestStart();
        }
        return LatencyStart::OutputFailed;
    }
    return LatencyStart::Ok;
}

void AudioHost::finishLatencyTest(bool gotInput) {
    std::lock_guard<std::mutex> guard(lock_);
    if (!input_) return;
    const auto frames = inputCallback_.frames();
    char status[96];
    snprintf(status, sizeof(status), "%lld échantillons reçus", static_cast<long long>(frames));
    describeInputLocked(kInputConfigs[currentInputConfig_].description, status);
    // This configuration gave nothing on this phone: try the next one next time.
    if (!gotInput) nextInputConfig_ = currentInputConfig_ + 1;
    closeLocked();
    // Streams are closed: no audio thread touches the meter now.
    engine_.latencyMeter().cancel();
    if (wantRunning_ && openOutputLocked()) {
        engine_.prepare(output_->getSampleRate());
        output_->requestStart();
    }
}

oboe::DataCallbackResult AudioHost::onAudioReady(oboe::AudioStream* stream, void* audioData,
                                                 int32_t numFrames) {
    auto* out = static_cast<float*>(audioData);
    const float* in = nullptr;
    int inFrames = 0;
    oboe::AudioStream* input = input_.get();
    if (input != nullptr) {
        // Drop samples captured before the output started so both directions start aligned.
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
    // Called on an Oboe-owned thread, e.g. when headphones are plugged in or unplugged.
    LOGW("stream closed: %s", oboe::convertToText(error));
    std::lock_guard<std::mutex> guard(lock_);
    if (stream != output_.get()) return;
    xrunBase_ = lastXruns_;
    output_.reset();
    if (input_) {
        input_->stop();
        input_->close();
        input_.reset();
    }
    engine_.latencyMeter().cancel();
    // A freshly (un)plugged device is often not ready yet: retry a few times.
    for (int attempt = 0; attempt < 5 && wantRunning_; ++attempt) {
        if (attempt > 0) std::this_thread::sleep_for(std::chrono::milliseconds(200));
        if (!openOutputLocked()) continue;
        engine_.prepare(output_->getSampleRate());
        if (output_->requestStart() == oboe::Result::OK) return;
        closeLocked();
    }
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
