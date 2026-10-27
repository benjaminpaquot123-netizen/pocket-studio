cmake_minimum_required(VERSION 3.22)
project(pocket_studio_engine_host_tests C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CORE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../src/main/cpp/core)
file(GLOB CORE_SOURCES CONFIGURE_DEPENDS ${CORE_DIR}/*.cpp ${CORE_DIR}/*.c)
include(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/daisysp.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/open303.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/libsamplerate.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/signalsmith.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/plaits.cmake)

add_library(ps_core STATIC ${CORE_SOURCES} ${DAISYSP_SOURCES} ${OPEN303_SOURCES} ${LIBSAMPLERATE_SOURCES} ${PLAITS_SOURCES})
target_include_directories(ps_core PUBLIC ${CORE_DIR})
target_include_directories(ps_core SYSTEM PUBLIC ${DAISYSP_INCLUDES} ${LIBSAMPLERATE_INCLUDES})
set_source_files_properties(${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp/core/AcidTrack.cpp ${CMAKE_CURRENT_LIST_DIR}/core/AcidTrack.cpp ${OPEN303_SOURCES} PROPERTIES INCLUDE_DIRECTORIES "${OPEN303_INCLUDES}")
set_source_files_properties(${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp/core/AcidTrack.cpp ${CMAKE_CURRENT_LIST_DIR}/core/AcidTrack.cpp PROPERTIES COMPILE_OPTIONS "-fno-strict-aliasing")
# Signalsmith (MIT, header-only): time-stretch, and the FFT of the analysers (onsets, tempo, key).
set_source_files_properties(${CORE_DIR}/TimeStretch.cpp ${CORE_DIR}/OnsetDetector.cpp ${CORE_DIR}/Analysis.cpp PROPERTIES INCLUDE_DIRECTORIES "${SIGNALSMITH_INCLUDES}")
# TinySoundFont (MIT, single header) is compiled in SoundFontPlayer.cpp only.
set_source_files_properties(${CORE_DIR}/SoundFontPlayer.cpp PROPERTIES INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/../third_party/TinySoundFont" COMPILE_OPTIONS "-Wno-unused-parameter;-Wno-unused-function;-Wno-sign-compare;-Wno-unused-variable")
target_compile_options(ps_core PUBLIC ${DAISYSP_FORCE_INCLUDE})
# Third-party code is not held to our warning level.
set_source_files_properties(${DAISYSP_SOURCES} PROPERTIES COMPILE_OPTIONS "-w")
# Open303 type-puns floats through pointers: never let the optimiser assume strict aliasing there.
set_source_files_properties(${OPEN303_SOURCES} PROPERTIES COMPILE_OPTIONS "-w;-fno-strict-aliasing")
# Same math flags as the device build (engine/src/main/cpp/CMakeLists.txt).
target_compile_options(ps_core PRIVATE -Wall -Wextra -Werror -O3 -ffast-math -fno-finite-math-only)

add_executable(engine_tests engine_tests.cpp)
target_link_libraries(engine_tests PRIVATE ps_core)
target_compile_options(engine_tests PRIVATE -Wall -Wextra -Werror)
target_compile_definitions(engine_tests PRIVATE
    PS_REFERENCE_DIR="${CMAKE_CURRENT_SOURCE_DIR}/reference"
    PS_SAMPLES_DIR="${CMAKE_CURRENT_SOURCE_DIR}/../../app/src/test/resources/real-projects/jumpup-dnb-20261024/samples"
    PS_OUTPUT_DIR="${CMAKE_CURRENT_BINARY_DIR}/renders")

enable_testing()
add_test(NAME engine_tests COMMAND engine_tests)

set_source_files_properties(${CORE_DIR}/PlaitsSynth.cpp ${CORE_DIR}/PlateReverb.cpp PROPERTIES COMPILE_OPTIONS "${PLAITS_SYSTEM_INCLUDE_FLAGS}" COMPILE_DEFINITIONS "${PLAITS_DEFINITIONS}")

# HIIR (WTFPL, header-only): oversampling in the master chain and the drive effects.
include(${CMAKE_CURRENT_SOURCE_DIR}/../third_party/hiir.cmake)
target_include_directories(ps_core SYSTEM PUBLIC ${HIIR_INCLUDES})

# Song-analysis corpus runner: writes onsets/tempo/key results as JSON (see analysis_corpus.h).
add_executable(analysis_reference analysis_reference.cpp)
target_link_libraries(analysis_reference PRIVATE ps_core)
target_compile_options(analysis_reference PRIVATE -Wall -Wextra -Werror)
target_compile_definitions(analysis_reference PRIVATE
    PS_SAMPLES_DIR="${CMAKE_CURRENT_SOURCE_DIR}/../../app/src/test/resources/real-projects/jumpup-dnb-20261024/samples")
