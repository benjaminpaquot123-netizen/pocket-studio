/* Force-included before every engine source (see daisysp.cmake).
 * DaisySP calls rand(), which takes a lock on Android (bionic): route it to a lock-free
 * generator. Standard headers that mention rand are included first, so they are unaffected. */
#pragma once
#include <stdlib.h>
#include <limits.h> /* Open303 uses INT_MAX, memcpy, memmove without including them */
#include <string.h>
#ifdef __cplusplus
#include <algorithm>
#include <cstdlib>
#include <random>
extern "C" int ps_fast_rand(void);
#else
int ps_fast_rand(void);
#endif
#define rand ps_fast_rand
