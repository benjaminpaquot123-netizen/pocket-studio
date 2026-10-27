// Song analysis (onsets, tempo, key) against the results of the previous analyser, aubio
// (GPL-3.0, removed so the app can be sold), recorded on the corpus of analysis_corpus.h in
// reference/analysis_aubio.json. Thresholds: 95 % of aubio's real attacks found again within
// 10 ms (aubio itself was 20-45 ms early on some synthetic hits: those count apart), at least as
// many true attacks as aubio; tempo within 0.5 BPM of the truth (another metrical level only
// where aubio was wrong too); same key as aubio on 90 % of the corpus.

#include <cctype>
#include <iterator>

#include "analysis_corpus.h"

namespace analysis_json {

// Just enough JSON for the reference file: objects, arrays, numbers, strings.
struct Value {
    enum class Kind { Null, Number, String, Array, Object } kind = Kind::Null;
    double number = 0;
    std::string text;
    std::vector<Value> items;
    std::vector<std::pair<std::string, Value>> fields;
    const Value& operator[](const std::string& key) const {
        static const Value none;
        for (const auto& f : fields) {
            if (f.first == key) return f.second;
        }
        return none;
    }
};

struct Parser {
    const std::string& s;
    size_t i = 0;
    void ws() { while (i < s.size() && std::isspace(static_cast<unsigned char>(s[i]))) ++i; }
    std::string str() {
        std::string out;
        ++i;  // "
        while (i < s.size() && s[i] != '"') out += s[i++];
        ++i;
        return out;
    }
    Value parse() {
        ws();
        Value v;
        if (i >= s.size()) return v;
        if (s[i] == '{') {
            v.kind = Value::Kind::Object;
            ++i;
            ws();
            while (i < s.size() && s[i] != '}') {
                ws();
                std::string key = str();
                ws();
                ++i;  // :
                v.fields.emplace_back(key, parse());
                ws();
                if (s[i] == ',') ++i;
                ws();
            }
            ++i;
        } else if (s[i] == '[') {
            v.kind = Value::Kind::Array;
            ++i;
            ws();
            while (i < s.size() && s[i] != ']') {
                v.items.push_back(parse());
                ws();
                if (s[i] == ',') ++i;
                ws();
            }
            ++i;
        } else if (s[i] == '"') {
            v.kind = Value::Kind::String;
            v.text = str();
        } else {
            v.kind = Value::Kind::Number;
            char* end = nullptr;
            v.number = std::strtod(s.c_str() + i, &end);
            i = static_cast<size_t>(end - s.c_str());
        }
        return v;
    }
};

inline Value load(const std::string& path) {
    std::ifstream in(path);
    std::string text((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
    Parser p{text};
    return p.parse();
}

}  // namespace analysis_json

namespace {

const analysis_json::Value& aubioReference() {
    static const analysis_json::Value ref = analysis_json::load(std::string(PS_REFERENCE_DIR) + "/analysis_aubio.json");
    return ref;
}

std::vector<float> readTestWav(const std::string& path, int& rate) {
    std::vector<float> x;
    if (!ps::readWavMono(path, rate, [&](const float* b, int n) { x.insert(x.end(), b, b + n); })) x.clear();
    return x;
}

}  // namespace

void testOnsetsMatchAubio() {
    const auto& ref = aubioReference()["onsets"];
    CHECK(ref.items.size() > 20);
    auto cases = corpus::syntheticOnsetCases();
    // The real samples of the jump-up project, by name.
    for (const auto& r : ref.items) {
        const std::string name = r["name"].text;
        if (name.rfind("sample_", 0) != 0) continue;
        corpus::OnsetCase c{name, 0, {}, {}};
        c.mono = readTestWav(std::string(PS_SAMPLES_DIR) + "/" + name.substr(7) + ".wav", c.rate);
        CHECK(!c.mono.empty());
        cases.push_back(std::move(c));
    }
    CHECK(cases.size() == ref.items.size());
    // Per sensitivity: aubio's onsets found again (all of them, and those on a real attack: where
    // the truth is known, aubio was sometimes 20 to 45 ms early), new onsets aubio did not have,
    // true attacks found by each.
    struct Level {
        const char* key;
        float sensitivity;
        int found = 0, total = 0, realFound = 0, realTotal = 0, extra = 0, mine = 0, truthFound = 0, truthAubio = 0, truthTotal = 0;
    };
    Level levels[] = {{"s25", 0.25f}, {"s50", 0.5f}, {"s75", 0.75f}, {"capped16", 0.5f}};
    for (size_t i = 0; i < cases.size() && i < ref.items.size(); ++i) {
        const auto& c = cases[i];
        const auto& r = ref.items[i];
        CHECK(r["name"].text == c.name);
        CHECK(static_cast<size_t>(r["frames"].number) == c.mono.size());
        const auto tol = static_cast<int64_t>(c.rate * 0.010);
        auto near = [&](int64_t x, const std::vector<int64_t>& in) {
            for (auto y : in) {
                if (std::llabs(x - y) <= tol) return true;
            }
            return false;
        };
        std::vector<int64_t> truth;
        for (double t : c.truth) {
            if (t > 0) truth.push_back(std::llround(t * c.rate));
        }
        for (auto& level : levels) {
            ps::OnsetOptions o;
            o.sensitivity = level.sensitivity;
            const bool capped = std::strcmp(level.key, "capped16") == 0;
            if (!capped) o.maxSlices = 100000;
            const auto mine = ps::detectOnsets(c.mono.data(), static_cast<int64_t>(c.mono.size()), c.rate, o);
            CHECK(!mine.empty() && mine.front() == 0);
            for (size_t k = 1; k < mine.size(); ++k) CHECK(mine[k] - mine[k - 1] >= static_cast<int64_t>(0.05f * c.rate) - 1);
            if (capped) CHECK(mine.size() <= 16);
            std::vector<int64_t> theirs;
            for (const auto& v : r[level.key].items) {
                if (v.number > 0) theirs.push_back(static_cast<int64_t>(v.number));
            }
            for (auto x : theirs) {
                ++level.total;
                const bool hit = near(x, mine);
                if (hit) ++level.found;
                if (truth.empty() || near(x, truth)) {
                    ++level.realTotal;
                    if (hit) ++level.realFound;
                }
            }
            for (auto x : mine) {
                if (x == 0) continue;
                ++level.mine;
                if (!near(x, theirs)) ++level.extra;
            }
            if (!capped) {
                for (auto t : truth) {
                    ++level.truthTotal;
                    if (near(t, mine)) ++level.truthFound;
                    if (near(t, theirs)) ++level.truthAubio;
                }
            }
        }
    }
    for (const auto& level : levels) {
        auto pct = [](int a, int b) { return b ? 100.0 * a / b : 100.0; };
        std::printf("  onsets %-8s: aubio's attacks found within 10 ms %d/%d (%.1f %%), its real ones %d/%d (%.1f %%); "
                    "%d of %d new ones not in aubio; true attacks found %d/%d (%.1f %%, aubio %.1f %%)\n",
                    level.key, level.found, level.total, pct(level.found, level.total), level.realFound, level.realTotal,
                    pct(level.realFound, level.realTotal), level.extra, level.mine, level.truthFound, level.truthTotal,
                    pct(level.truthFound, level.truthTotal), pct(level.truthAubio, level.truthTotal));
        if (std::strcmp(level.key, "capped16") == 0) {
            CHECK(pct(level.found, level.total) >= 85.0);  // which 15 are the "strongest" differs a little
            continue;
        }
        CHECK(pct(level.realFound, level.realTotal) >= 95.0);
        CHECK(pct(level.found, level.total) >= 85.0);
        CHECK(level.extra * 5 <= level.mine);  // not many more slices than before
        CHECK(level.truthFound >= level.truthAubio);
        CHECK(pct(level.truthFound, level.truthTotal) >= (level.sensitivity >= 0.5f ? 92.0 : 88.0));
    }
}

void testTempoMatchesAubio() {
    const auto& ref = aubioReference()["tempo"];
    const auto cases = corpus::tempoCases();
    CHECK(ref.items.size() == cases.size());
    auto fold = [](double b) {
        while (b < 70.0) b *= 2.0;
        while (b >= 180.0) b *= 0.5;
        return b;
    };
    auto related = [](double a, double b) {  // a is b at another metrical level
        for (double k : {0.5, 2.0, 2.0 / 3.0, 1.5}) {
            if (std::fabs(a - b * k) <= 0.5) return true;
        }
        return false;
    };
    int good = 0, sameAsAubio = 0, firstOk = 0, aubioGood = 0, exact = 0;
    for (size_t i = 0; i < cases.size() && i < ref.items.size(); ++i) {
        const auto& c = cases[i];
        const auto& r = ref.items[i];
        CHECK(r["name"].text == c.name);
        const auto x = corpus::renderGroove(c, 30.0);
        const auto t = ps::detectTempo(x.data(), static_cast<int64_t>(x.size()), c.rate);
        const double truth = fold(c.bpm), aubio = r["bpm"].number;
        // Right to 0.5 BPM; another metrical level only where aubio did not get it right either.
        const bool aubioRight = std::fabs(aubio - truth) <= 0.5;
        const bool ok = std::fabs(t.bpm - truth) <= 0.5 || (!aubioRight && related(t.bpm, truth));
        if (aubioRight) ++aubioGood;
        if (ok) ++good;
        if (std::fabs(t.bpm - truth) <= 0.5) ++exact;
        if (std::fabs(t.bpm - aubio) <= 0.5) ++sameAsAubio;
        // First beat on a kick: the phase of the grid, modulo one beat.
        const double period = 60.0 / c.bpm;
        double d = std::fmod(std::fabs(t.firstBeat - c.start), period);
        d = std::min(d, period - d);
        if (d < 0.03) ++firstOk;
        if (!ok) std::printf("  tempo %s: truth %.1f aubio %.1f new %.1f\n", c.name, truth, aubio, t.bpm);
    }
    std::printf("  tempo: %d / %zu within 0.5 BPM (aubio %d), %d more at another metrical level where aubio missed too, "
                "%d same as aubio, first beat on a kick %d\n",
                exact, cases.size(), aubioGood, good - exact, sameAsAubio, firstOk);
    CHECK(good == static_cast<int>(cases.size()));
    CHECK(exact >= aubioGood);
    CHECK(firstOk >= static_cast<int>(cases.size()) * 9 / 10);
}

void testKeyMatchesAubio() {
    const auto& ref = aubioReference()["key"];
    const auto cases = corpus::keyCases();
    CHECK(ref.items.size() == cases.size());
    int same = 0, truth = 0, aubioTruth = 0;
    for (size_t i = 0; i < cases.size() && i < ref.items.size(); ++i) {
        const auto& c = cases[i];
        const auto& r = ref.items[i];
        CHECK(r["name"].text == c.name);
        const auto x = corpus::renderKey(c);
        const auto k = ps::detectKey(x.data(), static_cast<int64_t>(x.size()), c.rate);
        if (k.tonic == static_cast<int>(r["tonic"].number) && k.minor == (r["minor"].number > 0.5)) ++same;
        if (k.tonic == c.tonic && k.minor == c.minor) ++truth;
        if (static_cast<int>(r["tonic"].number) == c.tonic && (r["minor"].number > 0.5) == c.minor) ++aubioTruth;
    }
    std::printf("  key: %d / %zu same as aubio; right %d (aubio %d)\n", same, cases.size(), truth, aubioTruth);
    CHECK(same * 10 >= static_cast<int>(cases.size()) * 9);
    CHECK(truth >= aubioTruth);
}

// The analysers never read past the buffer nor blow up on odd input.
void testAnalysisEdgeCases() {
    const int rate = 48000;
    // Shorter than one analysis window, a single sample, NaN and huge values.
    std::vector<float> tiny(100, 0.5f);
    CHECK(ps::detectOnsets(tiny.data(), 100, rate).size() >= 1);
    CHECK(ps::detectOnsets(tiny.data(), 1, rate).size() == 1);
    CHECK(ps::detectTempo(tiny.data(), 100, rate).bpm == 0.0f);
    std::vector<float> odd(static_cast<size_t>(rate) * 3, 0.0f);
    for (size_t i = 0; i < odd.size(); ++i) odd[i] = i % 4800 == 0 ? std::numeric_limits<float>::quiet_NaN() : (i % 9600 < 50 ? 1e6f : 0.0f);
    const auto on = ps::detectOnsets(odd.data(), static_cast<int64_t>(odd.size()), rate);
    CHECK(!on.empty() && on.front() == 0);
    for (size_t i = 1; i < on.size(); ++i) CHECK(on[i] > on[i - 1] && on[i] < static_cast<int64_t>(odd.size()));
    const auto t = ps::detectTempo(odd.data(), static_cast<int64_t>(odd.size()), rate);
    CHECK(std::isfinite(t.bpm) && std::isfinite(t.firstBeat) && std::isfinite(t.confidence));
    const auto k = ps::detectKey(odd.data(), static_cast<int64_t>(odd.size()), rate);
    CHECK(k.tonic >= -1 && k.tonic < 12 && std::isfinite(k.confidence));
    // Odd sample rates.
    for (int r : {8000, 22050, 96000}) {
        corpus::TempoCase c{"r", corpus::Groove::Four, 128.0, r, 0.1};
        const auto x = corpus::renderGroove(c, 20.0);
        const auto tr = ps::detectTempo(x.data(), static_cast<int64_t>(x.size()), r);
        std::printf("  tempo at %d Hz: %.1f\n", r, tr.bpm);
        CHECK(std::fabs(tr.bpm - 128.0f) <= 0.5f);
    }
}
