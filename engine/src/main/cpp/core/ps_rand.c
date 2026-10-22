/* Lock-free replacement for rand() inside DaisySP (compiled with -Drand=ps_fast_rand).
 * Bionic's rand() takes a mutex, which is forbidden on the audio thread.
 * One shared state, read and written with relaxed atomics: no thread-local storage, whose first
 * use on a new audio thread makes bionic allocate and lock (the library is loaded with dlopen).
 * Two threads may now and then draw the same number: harmless for noise. */
#include <stdatomic.h>
#include <stdint.h>
#include <stdlib.h>

static _Atomic uint32_t ps_rand_state = 0x9E3779B9u;

int ps_fast_rand(void) {
    uint32_t x = atomic_load_explicit(&ps_rand_state, memory_order_relaxed);
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;
    atomic_store_explicit(&ps_rand_state, x, memory_order_relaxed);
    return (int)(x % ((uint32_t)RAND_MAX + 1u));
}

/* The generator's state swapped for `state` (never 0); returns the one it had. A drum voice draws
 * from a state of its own (swapped in while it renders): its noise is then the same whatever the
 * callback's size and whichever voices play with it. */
uint32_t ps_rand_exchange(uint32_t state) {
    return atomic_exchange_explicit(&ps_rand_state, state ? state : 0x9E3779B9u, memory_order_relaxed);
}
