#include "AudioHost.h"

#include "ReopenBackoff.h"

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
    : engine_(kSampleRate), inputBuffer_(kMaxInputFrames, 0.0f), inputRing_(kInputRingSize), inputCallback_(inputRing_) {
    // A full resync spread over a few callbacks rather than one missing its deadline.
    engine_.setCommandBudget(Engine::kLiveCommandBudget);
}

AudioHost::~AudioHost() {
    stop();
    {
        std::lock_guard<std::mutex> guard(lock_);
        quitting_ = true;
    }
    reviveCv_.notify_all();
    if (reviver_.joinable()) reviver_.join();
}

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
    inputWarming_.store(false, std::memory_order_release);
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
    if (!wantRunning_) return;
    if (!openOutputLocked()) {
        scheduleReviveLocked();
        return;
    }
    engine_.prepare(output_->getSampleRate());
    // Revue neuve (MINEUR 1): a stream that does not start is not left open as if alive.
    if (output_->requestStart() != oboe::Result::OK) {
        closeLocked();
        scheduleReviveLocked();
    }
}

bool AudioHost::start() {
    std::lock_guard<std::mutex> guard(lock_);
    wantRunning_ = true;
    if (output_ && output_->getState() == oboe::StreamState::Started) return true;
    closeLocked();
    if (!openOutputLocked()) {
        // The device is not ready yet (a headset connecting while the app came back): keep trying.
        scheduleReviveLocked();
        return false;
    }
    engine_.prepare(output_->getSampleRate());
    if (output_->requestStart() != oboe::Result::OK) {
        closeLocked();
        scheduleReviveLocked();
        return false;
    }
    reviveWanted_ = false;
    return true;
}

void AudioHost::retryOutput() {
    // Never takes lock_ (the main thread's device callback): a wake-up missed meanwhile only means
    // the revival tries at its next delay.
    reviveNow_.store(true, std::memory_order_release);
    reviveCv_.notify_all();
}

void AudioHost::scheduleReviveLocked() {
    if (quitting_) return;
    reviveWanted_ = true;
    if (!reviver_.joinable()) reviver_ = std::thread([this] { reviveLoop(); });
    reviveCv_.notify_all();
}

bool AudioHost::reopenOutputLocked() {
    if (!openOutputLocked()) return false;
    // No audio thread runs here (the stream is not started yet): the engine may be prepared.
    engine_.prepare(output_->getSampleRate());
    if (output_->requestStart() != oboe::Result::OK) {
        closeLocked();
        return false;
    }
    // Opening the microphone rerouted the audio (some phones): a take keeps its microphone.
    if (recordingInput_) reopenRecordingInputLocked();
    return true;
}

void AudioHost::reviveLoop() {
    std::unique_lock<std::mutex> guard(lock_);
    int attempt = 0;
    while (!quitting_) {
        if (!reviveWanted_ || !wantRunning_ || output_) {
            // Nothing to revive (running again, or the app went away): wait for the next need.
            reviveWanted_ = false;
            attempt = 0;
            reviveNow_.store(false, std::memory_order_relaxed);
            reviveCv_.wait(guard, [this] { return quitting_ || reviveWanted_; });
            continue;
        }
        reviveCv_.wait_for(guard, std::chrono::milliseconds(ReopenBackoff::delayMs(attempt)), [this] {
            return quitting_ || !reviveWanted_ || reviveNow_.load(std::memory_order_acquire);
        });
        reviveNow_.store(false, std::memory_order_relaxed);
        if (quitting_ || !reviveWanted_ || !wantRunning_ || output_) continue;
        ++attempt;
        // Oboe still closing the old stream: its own error callback reopens it, or the next attempt.
        if (closingOutput_) continue;
        if (reopenOutputLocked()) {
            ++restarts_;
            reviveWanted_ = false;
            LOGI("output revived after %d attempt(s)", attempt);
        }
    }
}

void AudioHost::stop() {
    std::lock_guard<std::mutex> guard(lock_);
    wantRunning_ = false;
    reviveWanted_ = false;
    reviveCv_.notify_all();
    // Everything closes: nobody listens any more, and a later start must not reopen the microphone.
    micUsers_ = 0;
    recordingInput_ = false;
    closeLocked();
}

void AudioHost::setBufferBursts(int bursts) {
    std::lock_guard<std::mutex> guard(lock_);
    bufferBursts_ = bursts < 1 ? 1 : (bursts > 8 ? 8 : bursts);
    if (output_) output_->setBufferSizeInFrames(output_->getFramesPerBurst() * bufferBursts_);
}

bool AudioHost::post(const Command& command) {
    std::unique_lock<std::mutex> guard(postLock_);
    if (engine_.post(command)) return true;
    // Full: a big resync posts thousands of commands at once, and the audio thread empties the
    // queue at every callback. Wait for it (100 ms at most: Bluetooth callbacks can be 40 ms
    // apart) rather than drop a note. A stopped stream does not drain: after one wait without any
    // callback, give up at once until it runs. The lock is let go while waiting: a MIDI note
    // played meanwhile takes the next free place instead of queueing behind the whole resync
    // (audit 13 M1); each producer's own commands keep their order.
    const uint64_t seen = engine_.callbackCount();
    if (seen == stalledAt_) return false;
    for (int i = 0; i < 200; ++i) {
        guard.unlock();
        std::this_thread::sleep_for(std::chrono::microseconds(500));
        guard.lock();
        if (engine_.post(command)) return true;
    }
    if (engine_.callbackCount() == seen) stalledAt_ = seen;
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
        // Just reopened mid-take and nothing captured yet: no input at all, not an empty one.
        if (inputWarming_.load(std::memory_order_acquire)) {
            if (inFrames > 0) inputWarming_.store(false, std::memory_order_release);
            else in = nullptr;
        }
    }
    engine_.process(in, inFrames, 1, out, numFrames, stream->getChannelCount());
    return oboe::DataCallbackResult::Continue;
}

void AudioHost::onErrorBeforeClose(oboe::AudioStream* stream, oboe::Result error) {
    // Oboe is about to close this stream (disconnect): from now on nothing may call into it, so it
    // leaves output_ / input_ (every user checks those under lock_) until onErrorAfterClose.
    LOGW("stream closing: %s", oboe::convertToText(error));
    std::lock_guard<std::mutex> guard(lock_);
    if (output_ && stream == output_.get()) {
        xrunBase_ = lastXruns_;
        closingOutput_ = std::move(output_);
    } else if (input_ && stream == input_.get()) {
        inputLive_.store(false, std::memory_order_release);
        closingInput_ = std::move(input_);
    }
}

void AudioHost::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    // Called on an Oboe-owned thread, e.g. when headphones are plugged in or unplugged,
    // or when starting the microphone reroutes audio.
    LOGW("stream closed: %s", oboe::convertToText(error));
    std::lock_guard<std::mutex> guard(lock_);
    const bool wasOutput = (closingOutput_ && stream == closingOutput_.get()) || (output_ && stream == output_.get());
    const bool wasInput = (closingInput_ && stream == closingInput_.get()) || (input_ && stream == input_.get());
    if (!wasOutput && !wasInput) return;
    if ((input_ || closingInput_) && !recordingInput_) {
        // A latency test was running: report it instead of failing silently.
        testInterrupted_.store(true, std::memory_order_release);
    }
    // The old output closed while start() (or the revival) already opened a new one: nothing to
    // reopen (closing the new one would cut the sound a second time).
    if (wasOutput && !(output_ && stream == output_.get()) && output_) {
        closingOutput_.reset();
        return;
    }
    // Only the microphone went (another app took it, a headset unplugged) while the output plays
    // on: the music, its voices and its tails keep going; a take gets its microphone back (a
    // latency test reports the cut and finishLatencyTest restores the usual output).
    if (wasInput && !wasOutput && output_ && output_->getState() == oboe::StreamState::Started) {
        if (input_ && stream == input_.get()) input_.reset();
        closingInput_.reset();
        inputLive_.store(false, std::memory_order_release);
        if (recordingInput_) reopenRecordingInputLocked();
        return;
    }
    // Already closed by Oboe: dropped without another call into it.
    if (wasOutput) {
        if (output_ && stream == output_.get()) {
            xrunBase_ = lastXruns_;
            output_.reset();
        }
        closingOutput_.reset();
    } else {
        if (input_ && stream == input_.get()) input_.reset();
        closingInput_.reset();
    }
    closeLocked();
    engine_.latencyMeter().cancel();
    // A freshly (un)plugged device is often not ready yet: retry a few times.
    for (int attempt = 0; attempt < ReopenBackoff::kQuickAttempts && wantRunning_; ++attempt) {
        if (attempt > 0) std::this_thread::sleep_for(std::chrono::milliseconds(ReopenBackoff::kQuickDelayMs));
        if (reopenOutputLocked()) {
            ++restarts_;
            return;
        }
    }
    // Still nothing (a Bluetooth headset connecting can take seconds): never silent for good, the
    // revival thread keeps trying (audit 13 M3).
    if (wantRunning_) scheduleReviveLocked();
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
    inputWarming_.store(true, std::memory_order_release);
    inputLive_.store(true, std::memory_order_release);
    return true;
}

bool AudioHost::startRecordingInput(MicUser user) {
    std::lock_guard<std::mutex> guard(lock_);
    if (!wantRunning_ || !output_) return false;
    if (input_) {  // already listening
        micUsers_ |= user;
        return true;
    }
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
    micUsers_ |= user;
    inputWarming_.store(false, std::memory_order_release);
    drainInput_.store(true, std::memory_order_release);
    inputLive_.store(true, std::memory_order_release);
    return true;
}

void AudioHost::stopRecordingInput(MicUser user) {
    std::lock_guard<std::mutex> guard(lock_);
    micUsers_ &= ~user;
    if (micUsers_ != 0) return;  // someone else still listens
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
    if (!guard.owns_lock()) {
        std::lock_guard<std::mutex> last(infoLock_);
        return lastInfo_;
    }
    StreamInfo info;
    info.restarts = restarts_;
    if (!output_) {
        info.xruns = lastXruns_;
        std::lock_guard<std::mutex> last(infoLock_);
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
    std::lock_guard<std::mutex> last(infoLock_);
    lastInfo_ = info;
    return info;
}

}  // namespace ps
