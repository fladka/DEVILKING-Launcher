cmake_minimum_required(VERSION 3.22.1)

project("devilking_engine")

# 1. Turn off unnecessary Llama.cpp PC features to save mobile RAM and build time
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Build tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Build examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Build server" FORCE)

# 2. Add the llama.cpp engine we just downloaded
add_subdirectory(llama)

# 3. Build our bridge
add_library(
        devilking_engine
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

# 4. Fuse them all together
target_link_libraries(
        devilking_engine
        ${log-lib}
        llama)
