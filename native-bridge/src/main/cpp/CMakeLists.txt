cmake_minimum_required(VERSION 3.22.1)
project(cyrus_native_bridge LANGUAGES CXX)

add_library(
    cyrus_libretro_host
    SHARED
    cyrus_libretro_host.cpp
)

find_library(log-lib log)
find_library(dl-lib dl)

target_link_libraries(
    cyrus_libretro_host
    ${log-lib}
    ${dl-lib}
)
