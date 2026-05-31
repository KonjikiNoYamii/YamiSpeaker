# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.22.1-g37088a8)

# We name the project and the target for the ExternalProject_Add() call
# to something that will highlight to the user what we are working on if
# something goes wrong and an error message is produced.

project(libopus-populate NONE)



include(ExternalProject)
ExternalProject_Add(libopus-populate
                     "UPDATE_DISCONNECTED" "False" "URL" "https://ftp.osuosl.org/pub/xiph/releases/opus/opus-1.4.tar.gz" "URL_HASH" "SHA256=c9b32b4253be5ae63d1ff16eea06b94b5f0f2951b7a02aceef58e3a3ce49c51f"
                    SOURCE_DIR          "/home/goldendarkness/Documents/YamiSpeaker/android-client/app/.cxx/Debug/5l2j2t2k/arm64-v8a/_deps/libopus-src"
                    BINARY_DIR          "/home/goldendarkness/Documents/YamiSpeaker/android-client/app/.cxx/Debug/5l2j2t2k/arm64-v8a/_deps/libopus-build"
                    CONFIGURE_COMMAND   ""
                    BUILD_COMMAND       ""
                    INSTALL_COMMAND     ""
                    TEST_COMMAND        ""
                    USES_TERMINAL_DOWNLOAD  YES
                    USES_TERMINAL_UPDATE    YES
)


