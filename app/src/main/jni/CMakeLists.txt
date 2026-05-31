cmake_minimum_required(VERSION 3.22)
project(opus_jni_wrapper)

set(CMAKE_C_STANDARD 11)

include(FetchContent)
FetchContent_Declare(
    libopus
    URL https://ftp.osuosl.org/pub/xiph/releases/opus/opus-1.4.tar.gz
    URL_HASH SHA256=c9b32b4253be5ae63d1ff16eea06b94b5f0f2951b7a02aceef58e3a3ce49c51f
)
set(OPUS_INSTALL_CMAKE_PACKAGE OFF CACHE BOOL "")
set(OPUS_INSTALL_PKG_CONFIG_MODULE OFF CACHE BOOL "")
FetchContent_MakeAvailable(libopus)

add_library(opus_wrapper SHARED opus_wrapper.c)
target_include_directories(opus_wrapper PRIVATE ${libopus_SOURCE_DIR}/include)
target_link_libraries(opus_wrapper opus)
