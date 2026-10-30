cmake_minimum_required(VERSION 3.22.1)

# Native code is always built optimized: an -O0 build of whisper.cpp is
# dozens of times slower, which makes dictation unusable in debug builds.
set(CMAKE_BUILD_TYPE Release)

project(whisperjni C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# whisper.cpp and ggml as shared libraries. ggml builds one CPU backend per
# ARM feature level (dotprod, i8mm, SVE2, SME, ...) and loads the best one for
# the phone at run time.
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL ON CACHE BOOL "" FORCE)
set(GGML_CPU_ALL_VARIANTS ON CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    whisper
    URL https://github.com/ggml-org/whisper.cpp/archive/refs/tags/v1.9.5.tar.gz
    URL_HASH SHA256=ff1a9053feb509ff9d7729703355541ae9690073a6b1c40eb692c962e0dc1720
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE
)
FetchContent_MakeAvailable(whisper)

add_library(whisperjni SHARED whisper_jni.cpp)
target_compile_options(whisperjni PRIVATE -O3)
target_link_libraries(whisperjni PRIVATE whisper ggml android log)
