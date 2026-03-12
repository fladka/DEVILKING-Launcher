cmake_minimum_required(VERSION 3.22.1)
project("devilking")

add_library(devilking SHARED
        native-lib.cpp)

find_library(log-lib log)

target_link_libraries(devilking
        ${log-lib})
