// Separates one given song on Linux with the app's own separator (the real-song check of lot C):
// separate_file model.onnx song.raw channels rate outDir
// song.raw is interleaved 32-bit float (what scripts/separation-tests.sh --file decodes with
// ffmpeg); the stems are written to outDir/<stem>.wav (32-bit float, stereo, 44.1 kHz), as on
// the phone. The song and its stems never go into the repository: outDir is a scratch folder.
#include <cstdio>
#include <cstdlib>

#include "Separator.h"

int main(int argc, char** argv) {
    if (argc < 6) {
        std::fprintf(stderr, "usage: separate_file model.onnx song.raw channels rate outDir\n");
        return 2;
    }
    const int channels = std::atoi(argv[3]);
    const int rate = std::atoi(argv[4]);
    int lastTenth = -1;
    const auto r = ps::separateFile(argv[1], argv[2], channels, rate, argv[5], [&](float p) {
        const int tenth = static_cast<int>(p * 10);
        if (tenth != lastTenth) {
            lastTenth = tenth;
            std::printf("  separation %d %%\n", tenth * 10);
            std::fflush(stdout);
        }
        return true;
    }, 4);
    if (r.code != ps::SeparationResult::Ok) {
        std::fprintf(stderr, "SEPARATION FAILED (%d): %s\n", static_cast<int>(r.code), r.message.c_str());
        return 1;
    }
    std::printf("SEPARATED into %s\n", argv[5]);
    return 0;
}
