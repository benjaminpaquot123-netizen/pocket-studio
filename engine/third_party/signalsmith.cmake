# Signalsmith Stretch 1.4.0 + Signalsmith Linear 0.6.4 (MIT, header-only): time-stretch and
# pitch-shift of pad sounds, and the FFT of the analysers (onsets, tempo, key), off the audio
# thread. Visible to core/TimeStretch.cpp, OnsetDetector.cpp and Analysis.cpp only.
set(SIGNALSMITH_INCLUDES
    ${CMAKE_CURRENT_LIST_DIR}/signalsmith-stretch
    ${CMAKE_CURRENT_LIST_DIR}/signalsmith-linear/include)
