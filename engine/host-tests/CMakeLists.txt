cmake_minimum_required(VERSION 3.22)
project(pocket_studio_engine_host_tests CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CORE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../src/main/cpp/core)
file(GLOB CORE_SOURCES ${CORE_DIR}/*.cpp)

add_library(ps_core STATIC ${CORE_SOURCES})
target_include_directories(ps_core PUBLIC ${CORE_DIR})
target_compile_options(ps_core PRIVATE -Wall -Wextra -Werror)

add_executable(engine_tests engine_tests.cpp)
target_link_libraries(engine_tests PRIVATE ps_core)
target_compile_options(engine_tests PRIVATE -Wall -Wextra -Werror)
target_compile_definitions(engine_tests PRIVATE
    PS_REFERENCE_DIR="${CMAKE_CURRENT_SOURCE_DIR}/reference"
    PS_OUTPUT_DIR="${CMAKE_CURRENT_BINARY_DIR}/renders")

enable_testing()
add_test(NAME engine_tests COMMAND engine_tests)
