cmake_minimum_required(VERSION 3.22)
project(pocket_studio_song_bench C CXX)

# The song bench (docs/plan/son.md § 2): ps_render replays the engine commands of a real export
# (a .pscmd file recorded by the app's RecordingSink) through the engine core; ps_measure measures
# the result (loudness, true peak, bands, stereo, sections). Off-line tools: never in the app.

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# The engine core built exactly as the host tests build it (same sources and flags), tests left out.
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../host-tests ${CMAKE_CURRENT_BINARY_DIR}/core EXCLUDE_FROM_ALL)

# libebur128 1.2.6 (MIT, vendored): EBU R 128 loudness, LRA and true peak.
set(EBUR128_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../third_party/libebur128)
add_library(ebur128 STATIC ${EBUR128_DIR}/ebur128.c)
target_include_directories(ebur128 PUBLIC ${EBUR128_DIR} PRIVATE ${EBUR128_DIR}/queue)
set_target_properties(ebur128 PROPERTIES C_STANDARD 99)
target_compile_options(ebur128 PRIVATE -w -O2)
target_link_libraries(ebur128 PUBLIC m)

add_executable(ps_render ps_render.cpp)
target_link_libraries(ps_render PRIVATE ps_core)
target_compile_options(ps_render PRIVATE -Wall -Wextra -Werror -O2)

# Measurement code: plain IEEE maths (no -ffast-math), Signalsmith Linear's FFT (MIT, vendored).
add_executable(ps_measure ps_measure.cpp)
target_include_directories(ps_measure SYSTEM PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../third_party/signalsmith-linear/include)
target_link_libraries(ps_measure PRIVATE ebur128)
# The engine's own loudness pieces (header-only, real-time code): the Drop-mode target's LoudPart.
target_include_directories(ps_measure PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../src/main/cpp/core)
target_compile_options(ps_measure PRIVATE -Wall -Wextra -Werror -O2)
