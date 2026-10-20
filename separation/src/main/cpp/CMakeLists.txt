cmake_minimum_required(VERSION 3.22)
project(pocketstudio_separation C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# demucs.onnx (MIT) includes <onnxruntime/core/session/...>: give the AAR's flat headers that layout.
set(ORT_INCLUDE ${CMAKE_CURRENT_BINARY_DIR}/ort-include)
file(GLOB ORT_HEADERS ${ORT_DIR}/headers/*.h)
file(COPY ${ORT_HEADERS} DESTINATION ${ORT_INCLUDE}/onnxruntime/core/session)
file(COPY ${ORT_HEADERS} DESTINATION ${ORT_INCLUDE})

add_library(onnxruntime SHARED IMPORTED)
set_target_properties(onnxruntime PROPERTIES IMPORTED_LOCATION ${ORT_DIR}/jni/${ANDROID_ABI}/libonnxruntime.so)

set(DEMUCS ${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/demucs.onnx/src)
set(EIGEN ${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/eigen)

include(${CMAKE_CURRENT_SOURCE_DIR}/../../../../engine/third_party/libsamplerate.cmake)

add_library(pocketstudio_separation SHARED
    SeparationJni.cpp Separator.cpp PitchTranscriber.cpp AudioGenerator.cpp ${LIBSAMPLERATE_SOURCES}
    ${DEMUCS}/dsp.cpp ${DEMUCS}/model_apply.cpp ${DEMUCS}/model_inference.cpp)
target_include_directories(pocketstudio_separation PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${DEMUCS} ${ORT_INCLUDE} ${LIBSAMPLERATE_INCLUDES})
target_include_directories(pocketstudio_separation SYSTEM PRIVATE ${EIGEN})
target_compile_options(pocketstudio_separation PRIVATE -O3 -Wall -Wno-unused-variable -Wno-sign-compare)
set_source_files_properties(${DEMUCS}/dsp.cpp ${DEMUCS}/model_apply.cpp ${DEMUCS}/model_inference.cpp PROPERTIES COMPILE_OPTIONS "-w")
target_link_libraries(pocketstudio_separation PRIVATE onnxruntime log)
target_link_options(pocketstudio_separation PRIVATE "-Wl,-z,max-page-size=16384")
