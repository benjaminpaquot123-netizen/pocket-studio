// Host tool for the note instruments' fingerprint (docs/sons/fin/TIMBRE-INSTRUMENTS-OUTIL.md).
// Bench only: nothing in the app uses it. It plays the engine's own synth (core/Synth.cpp) the
// way a track does: one note (or a chord) held, then released, at 48 kHz, tempo 120.
//
//   render_instr   reads lines "hold_s total_s n midi1 .. midiN p0 .. p58" on stdin and writes,
//                  for each line, float32 stereo interleaved (total_s seconds) on stdout
#include <algorithm>
#include <cstdio>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

#include "Synth.h"

int main() {
    constexpr int kRate = 48000;
    constexpr int kBlock = 256;
    std::string line;
    while (std::getline(std::cin, line)) {
        if (line.empty()) continue;
        std::istringstream in(line);
        double hold = 0.0, total = 0.0;
        int n = 0;
        in >> hold >> total >> n;
        std::vector<int> notes(static_cast<size_t>(n));
        for (int& m : notes) in >> m;
        ps::Synth synth;
        synth.prepare(kRate);
        synth.setBeatFrames(kRate * 0.5);
        for (int i = 0; i < ps::Synth::ParamCount; ++i) {
            float v = 0.0f;
            if (!(in >> v)) {
                std::fprintf(stderr, "render_instr: %d values, %d expected\n", i, ps::Synth::ParamCount);
                return 2;
            }
            synth.setParam(static_cast<ps::Synth::Param>(i), v);
        }
        const long frames = static_cast<long>(total * kRate);
        const long off = static_cast<long>(hold * kRate);
        std::vector<float> left(kBlock), right(kBlock), out(2 * kBlock);
        for (int m : notes) synth.noteOn(m, 0.8f);
        for (long at = 0; at < frames; at += kBlock) {
            const int len = static_cast<int>(std::min<long>(kBlock, frames - at));
            if (off >= at && off < at + len)
                for (int m : notes) synth.noteOff(m, static_cast<int>(off - at));
            // render() adds into the buffers, and writes only `left` while the sound is mono.
            std::fill(left.begin(), left.end(), 0.0f);
            std::fill(right.begin(), right.end(), 0.0f);
            synth.render(left.data(), right.data(), len);
            if (!synth.stereo()) std::copy(left.begin(), left.end(), right.begin());
            for (int i = 0; i < len; ++i) {
                out[2 * i] = left[i];
                out[2 * i + 1] = right[i];
            }
            std::fwrite(out.data(), sizeof(float), static_cast<size_t>(2 * len), stdout);
        }
    }
    return 0;
}
