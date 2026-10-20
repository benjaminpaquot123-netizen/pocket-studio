# Mutable Instruments Plaits (MIT): 24-engine macro oscillator, including a DX7-style six-op FM.
# Vendored subset, see mutable/README.md. Real-time safe once initialised (static buffers).
set(MI_ROOT ${CMAKE_CURRENT_LIST_DIR}/mutable)
file(GLOB_RECURSE PLAITS_SOURCES CONFIGURE_DEPENDS ${MI_ROOT}/plaits/dsp/*.cc)
list(APPEND PLAITS_SOURCES ${MI_ROOT}/plaits/resources.cc ${MI_ROOT}/stmlib/dsp/atan.cc ${MI_ROOT}/stmlib/dsp/units.cc
    ${MI_ROOT}/stmlib/utils/random.cc)
# The shim comes first: it replaces plaits/user_data.h (flash storage on the module).
set(PLAITS_INCLUDES ${MI_ROOT}/shim ${MI_ROOT})
# For our own wrapper (built with warnings as errors): Plaits' headers as system headers.
set(PLAITS_SYSTEM_INCLUDE_FLAGS "-isystem;${MI_ROOT}/shim;-isystem;${MI_ROOT}")
# TEST selects stmlib's portable C paths instead of the module's ARM assembly.
set(PLAITS_DEFINITIONS TEST)
set_source_files_properties(${PLAITS_SOURCES} PROPERTIES
    COMPILE_DEFINITIONS "${PLAITS_DEFINITIONS}"
    COMPILE_OPTIONS "-w"
    INCLUDE_DIRECTORIES "${MI_ROOT}/shim;${MI_ROOT}")
