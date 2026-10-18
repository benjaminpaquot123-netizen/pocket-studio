/* Lock-free replacement for rand() inside DaisySP (compiled with -Drand=ps_fast_rand).
 * Bionic's rand() takes a mutex, which is forbidden on the audio thread.
 * Per-thread state: the live audio thread and an export worker each get their own. */
#include <stdint.h>
#include <stdlib.h>

static _Thread_local uint32_t ps_rand_state = 0x9E3779B9u;

int ps_fast_rand(void) {
    uint32_t x = ps_rand_state;
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;
    ps_rand_state = x;
    return (int)(x % ((uint32_t)RAND_MAX + 1u));
}
