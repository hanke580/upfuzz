#!/bin/bash

#NOTE THIS SCRIPT SHOULD ONLY BE RUN FROM INSIDE GRADLE
#HOWEVER IF NEEDED, SET first argument to your output directory for libnyxJNI.so

# set JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"
fi

# set path to JNI header files
JNI_INCLUDE="-I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux"

#set path to nyx_mode dir
NYX_MODE_DIR="nyx_mode"

# set the path to the libnyx.so file
#LIBNYX_SO_PATH="${NYX_MODE_DIR}/libnyx.so"

# set the path to the LibnyxInterface.c
LIBNYX_C_PATH="src/main/c/libnyx_interface/LibnyxInterface.c"

# set the output directory for the compiled files
OUTPUT_DIR=$1 # ex: "build/libs"

# compile the LibnyxInterface.c file into an object file
gcc -c -fPIC -o "${OUTPUT_DIR}/LibnyxInterface.o" -I${NYX_MODE_DIR} ${LIBNYX_C_PATH} ${JNI_INCLUDE}

# link the object file with the libnyx.so file to create the JNI library
gcc -shared -o "${OUTPUT_DIR}/libnyxJNI.so" "${OUTPUT_DIR}/LibnyxInterface.o" -L"${NYX_MODE_DIR}" -lnyx

# clean up the object file
rm "${OUTPUT_DIR}/LibnyxInterface.o"