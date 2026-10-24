cmake_minimum_required(VERSION 3.22)
project(pocketstudio_engine C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

file(GLOB CORE_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/core/*.cpp ${CMAKE_CURRENT_SOURCE_DIR}/core/*.c)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/daisysp.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/open303.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/libsamplerate.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/aubio.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/signalsmith.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/plaits.cmake)
file(GLOB ANDROID_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/android/*.cpp)

add_library(pocketstudio_engine SHARED ${CORE_SOURCES} ${ANDROID_SOURCES} ${DAISYSP_SOURCES} ${OPEN303_SOURCES} ${LIBSAMPLERATE_SOURCES} ${AUBIO_SOURCES} ${PLAITS_SOURCES})
target_include_directories(pocketstudio_engine PRIVATE core android)
target_include_directories(pocketstudio_engine SYSTEM PRIVATE ${DAISYSP_INCLUDES} ${LIBSAMPLERATE_INCLUDES})
set_source_files_properties(${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp/core/AcidTrack.cpp ${CMAKE_CURRENT_LIST_DIR}/core/AcidTrack.cpp ${OPEN303_SOURCES} PROPERTIES INCLUDE_DIRECTORIES "${OPEN303_INCLUDES}")
set_source_files_properties(${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp/core/AcidTrack.cpp ${CMAKE_CURRENT_LIST_DIR}/core/AcidTrack.cpp PROPERTIES COMPILE_OPTIONS "-fno-strict-aliasing")
# aubio headers (types.h, fvec.h…) are visible to the onset wrapper only.
set_source_files_properties(${CMAKE_CURRENT_SOURCE_DIR}/core/OnsetDetector.cpp ${CMAKE_CURRENT_SOURCE_DIR}/core/Analysis.cpp PROPERTIES INCLUDE_DIRECTORIES "${AUBIO_INCLUDES}")
set_source_files_properties(${CMAKE_CURRENT_SOURCE_DIR}/core/TimeStretch.cpp PROPERTIES INCLUDE_DIRECTORIES "${SIGNALSMITH_INCLUDES}")
# TinySoundFont (MIT, single header) is compiled in SoundFontPlayer.cpp only.
set_source_files_properties(${CMAKE_CURRENT_SOURCE_DIR}/core/SoundFontPlayer.cpp PROPERTIES INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/TinySoundFont" COMPILE_OPTIONS "-Wno-unused-parameter;-Wno-unused-function;-Wno-sign-compare;-Wno-unused-variable")
target_compile_options(pocketstudio_engine PRIVATE ${DAISYSP_FORCE_INCLUDE})
set_source_files_properties(${DAISYSP_SOURCES} PROPERTIES COMPILE_OPTIONS "-w")
# Open303 type-puns floats through pointers: never let the optimiser assume strict aliasing there.
set_source_files_properties(${OPEN303_SOURCES} PROPERTIES COMPILE_OPTIONS "-w;-fno-strict-aliasing")
target_compile_options(pocketstudio_engine PRIVATE -Wall -Wextra -O3 -ffast-math -fno-finite-math-only)
target_link_libraries(pocketstudio_engine PRIVATE oboe::oboe log android)
target_link_options(pocketstudio_engine PRIVATE "-Wl,-z,max-page-size=16384")

set_source_files_properties(${CMAKE_CURRENT_SOURCE_DIR}/core/PlaitsSynth.cpp ${CMAKE_CURRENT_SOURCE_DIR}/core/PlateReverb.cpp PROPERTIES COMPILE_OPTIONS "${PLAITS_SYSTEM_INCLUDE_FLAGS}" COMPILE_DEFINITIONS "${PLAITS_DEFINITIONS}")

# HIIR (WTFPL, header-only): oversampling in the master chain and the drive effects.
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/hiir.cmake)
target_include_directories(pocketstudio_engine SYSTEM PRIVATE ${HIIR_INCLUDES})
