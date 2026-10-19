# aubio 0.4.9 (GPL-3.0, allowed by the project rules): onset detection to slice sounds.
# Only the onset and tempo paths are compiled (FFT: bundled Ooura). Used off the audio thread only.
set(AUBIO_ROOT ${CMAKE_CURRENT_LIST_DIR}/aubio/src)
set(AUBIO_SOURCES
    ${AUBIO_ROOT}/fvec.c ${AUBIO_ROOT}/cvec.c ${AUBIO_ROOT}/lvec.c
    ${AUBIO_ROOT}/mathutils.c ${AUBIO_ROOT}/musicutils.c ${AUBIO_ROOT}/vecutils.c
    ${AUBIO_ROOT}/onset/onset.c ${AUBIO_ROOT}/onset/peakpicker.c
    ${AUBIO_ROOT}/tempo/tempo.c ${AUBIO_ROOT}/tempo/beattracking.c
    ${AUBIO_ROOT}/spectral/specdesc.c ${AUBIO_ROOT}/spectral/phasevoc.c ${AUBIO_ROOT}/spectral/fft.c
    ${AUBIO_ROOT}/spectral/ooura_fft8g.c ${AUBIO_ROOT}/spectral/awhitening.c ${AUBIO_ROOT}/spectral/statistics.c
    ${AUBIO_ROOT}/temporal/filter.c ${AUBIO_ROOT}/temporal/biquad.c
    ${AUBIO_ROOT}/utils/hist.c ${AUBIO_ROOT}/utils/scale.c ${AUBIO_ROOT}/utils/log.c
)
set(AUBIO_INCLUDES ${AUBIO_ROOT})
set_source_files_properties(${AUBIO_SOURCES} PROPERTIES
    COMPILE_DEFINITIONS "HAVE_STDLIB_H;HAVE_STDIO_H;HAVE_MATH_H;HAVE_STRING_H;HAVE_LIMITS_H;HAVE_STDARG_H;HAVE_ERRNO_H;HAVE_C99_VARARGS_MACROS"
    COMPILE_OPTIONS "-w"
    INCLUDE_DIRECTORIES "${AUBIO_ROOT}")
