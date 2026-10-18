#include "AudioHost.h"

#include <android/log.h>

#include <algorithm>
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
constexpr int kMaxInputChannels = 2;

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

AudioHost::AudioHost() : engine_(kSampleRate), inputBuffer_(kMaxInputFrames * kMaxInputChannels, 0.0f) {}

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

bool AudioHost::openInputLocked() {
    for (const auto& config : kInputConfigs) {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(config.sharing)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(1)
            ->setSampleRate(kSampleRate)
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
            ->setInputPreset(config.preset);
        const auto result = builder.openStream(input_);
        if (result != oboe::Result::OK) {
            LOGW("openStream(input, %s) failed: %s", config.description, oboe::convertToText(result));
            input_.reset();
            continue;
        }
        // inputBuffer_ holds kMaxInputFrames frames of up to kMaxInputChannels channels.
        if (input_->getChannelCount() > kMaxInputChannels) {
            input_->close();
            input_.reset();
            continue;
        }
        inputChannels_ = input_->getChannelCount();
        inputDescription_ = config.description;
        LOGI("input: %s, rate=%d channels=%d", config.description, input_->getSampleRate(), inputChannels_);
        return true;
    }
    inputDescription_ = "impossible à ouvrir";
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
    input_->requestStart();
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

void AudioHost::finishLatencyTest() {
    std::lock_guard<std::mutex> guard(lock_);
    if (!input_) return;
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
        if (drainInput_.exchange(false, std::memory_order_acq_rel)) {
            // Drop stale input so both directions start aligned.
            for (int i = 0; i < 16; ++i) {
                auto drained = input->read(inputBuffer_.data(), kMaxInputFrames, 0);
                if (!drained || drained.value() == 0) break;
            }
        }
        // Take everything the microphone has delivered so far, without blocking.
        // The meter records it back to back, so uneven chunks don't distort the timing.
        while (inFrames < kMaxInputFrames) {
            auto read = input->read(inputBuffer_.data() + inFrames * inputChannels_, kMaxInputFrames - inFrames, 0);
            if (!read || read.value() <= 0) break;
            inFrames += read.value();
        }
        in = inputBuffer_.data();
    }
    engine_.process(in, inFrames, inputChannels_, out, numFrames, stream->getChannelCount());
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
