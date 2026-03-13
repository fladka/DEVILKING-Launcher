cmake_minimum_required(VERSION 3.22.1)

project("devilking_engine")

# We will add the actual llama.cpp source files here later
add_library(
        devilking_engine
        SHARED
        native-lib.cpp)

# Link to standard Android logging so the C++ engine can print to your terminal
find_library(
        log-lib
        log)

target_link_libraries(
        devilking_engine
        ${log-lib})
