#pragma once

#include <cstddef>
#include <string>
#include <vector>

namespace ps {

// Audio-to-MIDI transcription with Spotify's Basic Pitch (ICASSP 2022 model, ONNX, Apache-2.0).
// A faithful port of basic_pitch.inference.predict() with its default settings (windowing,
// unwrap_output, output_to_notes_polyphonic with inferred onsets and the melodia trick,
// model_frames_to_time). Seconds of CPU; run on a worker thread, never near the audio engine.

struct PitchNote {
    double start = 0.0;    // seconds
    double end = 0.0;      // seconds
    int pitch = 0;         // MIDI note number (21..108)
    float amplitude = 0;   // mean note activation, 0..1 (upstream maps it to velocity * 127)
};

// SHA-256 of basic_pitch/saved_models/icassp_2022/nmp.onnx (upstream v0.4.0 / commit fa5997a).
// The model file is hashed before ONNX Runtime ever parses it.
constexpr const char* kBasicPitchSha256 = "2c3c1d144bfa61ad236e92e169c13535c880469a12a047d4e73451f2c059a0ec";
constexpr int kBasicPitchSampleRate = 22050;
// Longest input accepted (keeps memory and time bounded on a phone).
constexpr double kBasicPitchMaxSeconds = 600.0;

// `mono` at any `sampleRate` (resampled to 22.05 kHz). Returns the notes sorted by start then
// pitch; on failure returns an empty list and sets `*error` (French, shown to the user).
// An empty list with an empty error means "no note found". `threads` <= 0 picks a default.
std::vector<PitchNote> transcribe(const std::string& modelPath, const float* mono, size_t frames, int sampleRate,
                                  std::string* error, int threads = 0);

// Lower-case hex SHA-256 of `size` bytes (exposed for tests).
std::string sha256Hex(const unsigned char* data, size_t size);

}  // namespace ps
