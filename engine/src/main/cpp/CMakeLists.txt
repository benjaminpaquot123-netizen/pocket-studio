cmake_minimum_required(VERSION 3.22)
project(pocketstudio_engine C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

file(GLOB CORE_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/core/*.cpp ${CMAKE_CURRENT_SOURCE_DIR}/core/*.c)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/daisysp.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/open303.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/libsamplerate.cmake)
file(GLOB ANDROID_SOURCES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/android/*.cpp)

add_library(pocketstudio_engine SHARED ${CORE_SOURCES} ${ANDROID_SOURCES} ${DAISYSP_SOURCES} ${OPEN303_SOURCES} ${LIBSAMPLERATE_SOURCES})
target_include_directories(pocketstudio_engine PRIVATE core android)
target_include_directories(pocketstudio_engine SYSTEM PRIVATE ${DAISYSP_INCLUDES} ${LIBSAMPLERATE_INCLUDES})
set_source_files_properties(${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp/core/AcidTrack.cpp ${CMAKE_CURRENT_LIST_DIR}/core/AcidTrack.cpp ${OPEN303_SOURCES} PROPERTIES INCLUDE_DIRECTORIES "${OPEN303_INCLUDES}")
set_source_files_properties(${CMAKE_CURRENT_LIST_DIR}/../src/main/cpp/core/AcidTrack.cpp ${CMAKE_CURRENT_LIST_DIR}/core/AcidTrack.cpp PROPERTIES COMPILE_OPTIONS "-fno-strict-aliasing")
target_compile_options(pocketstudio_engine PRIVATE ${DAISYSP_FORCE_INCLUDE})
set_source_files_properties(${DAISYSP_SOURCES} PROPERTIES COMPILE_OPTIONS "-w")
# Open303 type-puns floats through pointers: never let the optimiser assume strict aliasing there.
set_source_files_properties(${OPEN303_SOURCES} PROPERTIES COMPILE_OPTIONS "-w;-fno-strict-aliasing")
target_compile_options(pocketstudio_engine PRIVATE -Wall -Wextra -O3 -ffast-math -fno-finite-math-only)
target_link_libraries(pocketstudio_engine PRIVATE oboe::oboe log android)
target_link_options(pocketstudio_engine PRIVATE "-Wl,-z,max-page-size=16384")
