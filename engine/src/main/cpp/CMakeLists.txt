cmake_minimum_required(VERSION 3.22)
project(pocketstudio_engine CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

file(GLOB CORE_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/core/*.cpp)
file(GLOB ANDROID_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/android/*.cpp)

add_library(pocketstudio_engine SHARED ${CORE_SOURCES} ${ANDROID_SOURCES})
target_include_directories(pocketstudio_engine PRIVATE core android)
target_compile_options(pocketstudio_engine PRIVATE -Wall -Wextra -O3 -ffast-math -fno-finite-math-only)
target_link_libraries(pocketstudio_engine PRIVATE oboe::oboe log android)
target_link_options(pocketstudio_engine PRIVATE "-Wl,-z,max-page-size=16384")
