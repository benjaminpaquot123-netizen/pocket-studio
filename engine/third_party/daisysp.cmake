# DaisySP (MIT) sources used by the engine. Only the modules we need are compiled.
set(DAISYSP_ROOT ${CMAKE_CURRENT_LIST_DIR}/DaisySP/Source)
set(DAISYSP_SOURCES
    ${DAISYSP_ROOT}/Drums/analogbassdrum.cpp
    ${DAISYSP_ROOT}/Drums/analogsnaredrum.cpp
    ${DAISYSP_ROOT}/Drums/hihat.cpp
    ${DAISYSP_ROOT}/Drums/synthbassdrum.cpp
    ${DAISYSP_ROOT}/Drums/synthsnaredrum.cpp
    ${DAISYSP_ROOT}/Filters/svf.cpp
    ${DAISYSP_ROOT}/Filters/ladder.cpp
    ${DAISYSP_ROOT}/Control/adsr.cpp
    ${DAISYSP_ROOT}/Synthesis/oscillator.cpp
)
set(DAISYSP_INCLUDES ${DAISYSP_ROOT} ${DAISYSP_ROOT}/Utility)
# DaisySP calls rand(), which takes a lock on Android: every engine source force-includes
# an override that routes it to our lock-free generator (core/ps_rand.c).
set(DAISYSP_FORCE_INCLUDE "SHELL:-include ${CMAKE_CURRENT_LIST_DIR}/daisysp_rand_override.h")
