# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/share_libs/include/ev) # 头文件
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/share_libs/include/xuqic) # 头文件
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/main/include) # 头文件

add_compile_options(-fno-rtti -O3)

message(@@@${CMAKE_CURRENT_SOURCE_DIR})


link_directories(${CMAKE_CURRENT_SOURCE_DIR}/share_libs/${CMAKE_ANDROID_ARCH_ABI}) # 链接库地址
link_libraries(ev.so xquic.so)

aux_source_directory(. SRC_LIST) # 搜索当前目录下的所有.cpp文件
aux_source_directory(./main MAIN_SRC_LIST) # 搜索当前目录下的所有.cpp文件

# Declares and names the project.

project("xquicdemo")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             xnet-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${SRC_LIST} ${MAIN_SRC_LIST})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       xnet-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib})