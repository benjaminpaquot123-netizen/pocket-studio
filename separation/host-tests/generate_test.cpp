// Runs the real Stable Audio Open Small models (a directory with cond_q8.onnx, dit_l8.onnx,
// dec.onnx): a kick prompt gives a short, finite, bass-heavy sound; cancelling stops at once.
#include <cmath>
#include <cstdio>
#include <vector>

#include "AudioGenerator.h"

static int failures = 0;
#define CHECK(c) do { if (!(c)) { std::printf("  FAILED %s:%d: %s\n", __FILE__, __LINE__, #c); ++failures; } } while (0)

int main(int argc, char** argv) {
    if (argc < 2) return 2;
    ps::GenerateRequest r;
    r.dir = argv[1];
    r.tokens = {11462, 63, 4583, 5253, 80, 2538, 1};  // "punchy kick drum one shot" (t5-base)
    r.seconds = 1.0f;
    r.seed = 4;
    std::vector<float> out;
    float last = 0.0f;
    const auto result = ps::generateAudio(r, out, [&](float p) { last = p; return true; });
    CHECK(result == ps::GenerateResult::Ok);
    CHECK(out.size() == 44100 * 2);
    CHECK(last == 1.0f);
    double sum = 0.0, low = 0.0, peak = 0.0, prev = 0.0;
    for (size_t i = 0; i < out.size(); i += 2) {
        const double m = 0.5 * (out[i] + out[i + 1]);
        CHECK(std::isfinite(m));
        sum += m * m;
        peak = std::max(peak, std::fabs(m));
        // Energy of a one-pole low-pass (about 200 Hz): a kick lives down there.
        prev += 0.028 * (m - prev);
        low += prev * prev;
    }
    std::printf("  generated: rms %.3f, peak %.3f, low share %.2f\n", std::sqrt(sum / 44100), peak, low / std::max(sum, 1e-12));
    CHECK(std::sqrt(sum / 44100) > 0.02);
    CHECK(low / sum > 0.4);
    // Cancelled during the first denoising step: nothing comes out.
    const auto cancelled = ps::generateAudio(r, out, [](float p) { return p < 0.1f; });
    CHECK(cancelled == ps::GenerateResult::Cancelled && out.empty());
    std::printf(failures ? "GENERATE FAILED\n" : "GENERATE OK\n");
    return failures ? 1 : 0;
}
