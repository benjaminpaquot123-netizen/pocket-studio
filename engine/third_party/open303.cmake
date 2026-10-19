# Open303 (Robin Schmidt, MIT) DSP sources. Headers are only included from AcidTrack.cpp:
# they define global macros (PI, INLINE...) we keep out of the rest of the engine.
set(OPEN303_ROOT ${CMAKE_CURRENT_LIST_DIR}/Open303/Source/DSPCode)
file(GLOB OPEN303_SOURCES ${OPEN303_ROOT}/*.cpp ${OPEN303_ROOT}/*.c)
set(OPEN303_INCLUDES ${OPEN303_ROOT})

# Patch, applied to a copy so the submodule stays pristine: initPrototypeTable() clears
# tableLength+4 entries of a tableLength array (4 doubles written past its end, found by UBSan).
set(_wt ${OPEN303_ROOT}/rosic_MipMappedWaveTable.cpp)
file(READ ${_wt} _wt_content)
string(REGEX REPLACE "(for\\(int i=0; i<)\\(tableLength\\+4\\)(; i\\+\\+\\)[ \t\r\n]*prototypeTable\\[i\\] = 0\\.0;)"
       "\\1tableLength\\2" _wt_patched "${_wt_content}")
if(_wt_patched STREQUAL _wt_content)
    message(FATAL_ERROR "Open303 patch no longer applies: check rosic_MipMappedWaveTable.cpp")
endif()
set(_patched_dir ${CMAKE_CURRENT_BINARY_DIR}/open303_patched)
file(WRITE ${_patched_dir}/rosic_MipMappedWaveTable.cpp "${_wt_patched}")
list(REMOVE_ITEM OPEN303_SOURCES ${_wt})
list(APPEND OPEN303_SOURCES ${_patched_dir}/rosic_MipMappedWaveTable.cpp)
