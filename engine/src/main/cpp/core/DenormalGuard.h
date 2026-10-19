#pragma once

#include <cstdint>

#if defined(__SSE__) || defined(__x86_64__)
#include <xmmintrin.h>
#endif

namespace ps {

// Flushes denormal floats to zero for the scope of an audio callback. Decaying filters and
// envelopes produce tiny values that are extremely slow on some CPUs (x86 in particular).
class DenormalGuard {
public:
    DenormalGuard() {
#if defined(__aarch64__)
        uint64_t fpcr;
        asm volatile("mrs %0, fpcr" : "=r"(fpcr));
        saved_ = fpcr;
        asm volatile("msr fpcr, %0" : : "r"(fpcr | (1ULL << 24)));  // FZ
#elif defined(__SSE__) || defined(__x86_64__)
        saved_ = _mm_getcsr();
        _mm_setcsr(static_cast<unsigned int>(saved_) | 0x8040);  // FTZ | DAZ
#endif
    }
    ~DenormalGuard() {
#if defined(__aarch64__)
        asm volatile("msr fpcr, %0" : : "r"(saved_));
#elif defined(__SSE__) || defined(__x86_64__)
        _mm_setcsr(static_cast<unsigned int>(saved_));
#endif
    }
    DenormalGuard(const DenormalGuard&) = delete;
    DenormalGuard& operator=(const DenormalGuard&) = delete;

private:
    uint64_t saved_ = 0;
};

}  // namespace ps
