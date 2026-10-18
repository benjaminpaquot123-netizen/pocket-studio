#include "AudioHost.h"

#include <android/log.h>

#include <algorithm>

#define LOG_TAG "PocketStudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace ps {

namespace {
constexpr int kSampleRate = 48000;
constexpr int kOutputChannels = 2;
constexpr int kMaxInputFrames = 8192;
}  // namespace

AudioHost::AudioHost() : engine_(kSampleRate), inputBuffer_(kMaxInputFrames, 0.0f) {}

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
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(1)
        ->setSampleRate(kSampleRate)
        ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
        ->setInputPreset(oboe::InputPreset::Unprocessed);
    const auto result = builder.openStream(input_);
    if (result != oboe::Result::OK) {
        LOGW("openStream(input) failed: %s", oboe::convertToText(result));
        input_.reset();
        return false;
    }
    return true;
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
    if (output_) return true;
    if (!openOutputLocked()) return false;
    engine_.prepare(output_->getSampleRate());
    return output_->requestStart() == oboe::Result::OK;
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

bool AudioHost::startLatencyTest() {
    std::lock_guard<std::mutex> guard(lock_);
    closeLocked();
    if (!openInputLocked() || !openOutputLocked()) {
        closeLocked();
        if (wantRunning_ && openOutputLocked()) output_->requestStart();
        return false;
    }
    engine_.prepare(output_->getSampleRate());
    input_->requestStart();
    drainInput_.store(true, std::memory_order_release);
    {
        std::lock_guard<std::mutex> postGuard(postLock_);
        engine_.post({CommandType::StartLatencyTest, 0.0f});
    }
    return output_->requestStart() == oboe::Result::OK;
}

void AudioHost::finishLatencyTest() {
    std::lock_guard<std::mutex> guard(lock_);
    if (!input_) return;
    closeLocked();
    if (wantRunning_ && openOutputLocked()) output_->requestStart();
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
        const int wanted = std::min(numFrames, kMaxInputFrames);
        auto read = input->read(inputBuffer_.data(), wanted, 0);
        inFrames = read ? read.value() : 0;
        if (inFrames < wanted) std::fill(inputBuffer_.begin() + inFrames, inputBuffer_.begin() + wanted, 0.0f);
        in = inputBuffer_.data();
        inFrames = wanted;
    }
    engine_.process(in, inFrames, 1, out, numFrames, stream->getChannelCount());
    return oboe::DataCallbackResult::Continue;
}

void AudioHost::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    // Called on an Oboe-owned thread, e.g. when headphones are plugged in or unplugged.
    LOGW("stream closed: %s", oboe::convertToText(error));
    std::lock_guard<std::mutex> guard(lock_);
    if (stream != output_.get()) return;
    output_.reset();
    if (input_) {
        input_->stop();
        input_->close();
        input_.reset();
    }
    if (wantRunning_ && openOutputLocked()) output_->requestStart();
}

StreamInfo AudioHost::info() {
    std::lock_guard<std::mutex> guard(lock_);
    StreamInfo info;
    if (!output_) {
        info.xruns = lastXruns_;
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
    return info;
}

}  // namespace ps
