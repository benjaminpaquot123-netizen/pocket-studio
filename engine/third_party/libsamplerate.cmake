# libsamplerate (BSD-2-Clause): high-quality sample-rate conversion of imported audio.
# Used off the audio thread only (Sample::create).
set(LIBSAMPLERATE_ROOT ${CMAKE_CURRENT_LIST_DIR}/libsamplerate)
set(LIBSAMPLERATE_SOURCES
    ${LIBSAMPLERATE_ROOT}/src/samplerate.c
    ${LIBSAMPLERATE_ROOT}/src/src_sinc.c
    ${LIBSAMPLERATE_ROOT}/src/src_linear.c
    ${LIBSAMPLERATE_ROOT}/src/src_zoh.c
)
set(LIBSAMPLERATE_INCLUDES ${LIBSAMPLERATE_ROOT}/include)
set_source_files_properties(${LIBSAMPLERATE_SOURCES} PROPERTIES
    COMPILE_DEFINITIONS "ENABLE_SINC_BEST_CONVERTER;ENABLE_SINC_MEDIUM_CONVERTER;ENABLE_SINC_FAST_CONVERTER;HAVE_STDBOOL_H;PACKAGE=\"libsamplerate\";VERSION=\"0.2.2\""
    COMPILE_OPTIONS "-w"
    INCLUDE_DIRECTORIES "${LIBSAMPLERATE_ROOT}/include;${LIBSAMPLERATE_ROOT}/src")
