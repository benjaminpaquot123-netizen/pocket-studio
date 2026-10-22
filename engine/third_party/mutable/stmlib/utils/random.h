// Copyright 2012 Emilie Gillet.
//
// Author: Emilie Gillet (emilie.o.gillet@gmail.com)
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
// 
// See http://creativecommons.org/licenses/MIT/ for more information.
//
// -----------------------------------------------------------------------------
//
// Fast 16-bit pseudo random number generator.

#ifndef STMLIB_UTILS_RANDOM_H_
#define STMLIB_UTILS_RANDOM_H_

#include "stmlib/stmlib.h"

#include <atomic>

namespace stmlib {

// Pocket Studio change: the state is a relaxed atomic (it was a plain static). The live engine's
// audio thread and an export or preview engine's thread draw from it at the same time: a plain
// variable written by two threads is a data race (undefined behaviour). Relaxed loads and stores
// add no lock and no allocation on the audio thread; a voice keeps its own sequence by swapping
// its state in while it renders (ps::PlaitsSynth::render). Thread-local storage is avoided on
// purpose: its first use on a new thread makes bionic allocate and lock (the library is dlopened).

class Random {
 public:
  static inline uint32_t state() { return rng_state_.load(std::memory_order_relaxed); }

  static inline void Seed(uint32_t seed) {
    rng_state_.store(seed, std::memory_order_relaxed);
  }

  // Swaps the state for `seed`; returns the one it had (one atomic step).
  static inline uint32_t Exchange(uint32_t seed) {
    return rng_state_.exchange(seed, std::memory_order_relaxed);
  }

  static inline uint32_t GetWord() {
    const uint32_t next = state() * 1664525L + 1013904223L;
    rng_state_.store(next, std::memory_order_relaxed);
    return next;
  }
  
  static inline int16_t GetSample() {
    return static_cast<int16_t>(GetWord() >> 16);
  }

  static inline float GetFloat() {
    return static_cast<float>(GetWord()) / 4294967296.0f;
  }

 private:
  static std::atomic<uint32_t> rng_state_;

  DISALLOW_COPY_AND_ASSIGN(Random);
};

}  // namespace stmlib

#endif  // STMLIB_UTILS_RANDOM_H_
