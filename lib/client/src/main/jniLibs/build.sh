#!/bin/bash
#
# build GMP and libjbigi.so using the Android NDK toolchain
#
# Updated for NDK r28+ with 16KB page alignment support
# (required for Google Play Store compliance)
#
# Alternatively, use the following in i2p.i2p source core/c/jbigi:
# TARGET=android BITS=32 mbuild_all.sh
# TARGET=android BITS=64 mbuild_all.sh
#

# uncomment to skip
# exit 0

## works on linux and other unixes, but not osx.
if [ "`uname -s`" != "Darwin" ]; then
    THISDIR=$(dirname $(readlink -ne $0))
else
    THISDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi
cd $THISDIR

I2PBASE=${1:-$THISDIR/../../../../../i2p.i2p}
if [ ! -d "$I2PBASE" ]; then
    echo "I2P source not found in $I2PBASE"
    if [ -z "$1" ]; then
        echo "Extract it there or provide a path:"
        echo "./build.sh path/to/i2p.i2p"
    else
        echo "Extract it there or fix the supplied path"
    fi
    exit 1
fi

ROUTERJARS=$THISDIR/../../../../../routerjars

## Check the local.properties file first
export NDK=$(awk -F= '/ndk\.dir/{print $2}' "$ROUTERJARS/local.properties")

if [ "$NDK" == "" ]; then
## Simple fix for osx development
    if [ `uname -s` = "Darwin" ]; then
        export NDK="/Developer/android/ndk/"
    else
#
# We want to be able to not have to update this script
# every time a new NDK comes out. We solve this by using readlink with
# a wild card, deglobbing automatically sorts to get the highest revision.
# the dot at the end ensures that it is a directory, and not a file.
#
        NDK_GLOB=$THISDIR/'../../../../../android-ndk-r*/.'
        export NDK="`readlink -n -e $(for last in $NDK_GLOB; do true; done ; echo $last)`"
    fi

    if [ "$NDK" == "" ]; then
        echo "Cannot find NDK in $NDK_GLOB"
        echo "Install it here, or set ndk.dir in $ROUTERJARS/local.properties, or adjust NDK_GLOB in script"
        exit 1
    fi
fi

if [ ! -d "$NDK" ]; then
    echo "Cannot find NDK in $NDK, install it"
    exit 1
fi

JBIGI="$I2PBASE/core/c/jbigi"
#
# GMP Version
#
# prelim stats on a droid
# java (libcrypto) 29 ms
# 4.3.2 (jbigi) 34 ms
# 5.0.2 (jbigi) 32 ms
# libcrypto crashes on emulator, don't trust it
# jbigi about 20-25% slower than java on emulator
#
GMPVER=6.2.1
GMP="$JBIGI/gmp-$GMPVER"

if [ ! -d "$GMP" ]; then
    echo "Source dir for GMP version $GMPVER not found in $GMP"
    echo "Install it there or change GMPVER and/or GMP in this script"
    exit 1
fi

# Apply necessary patch
patch -d $GMP -p1 <gmp_thumb_add_mssaaaa.patch

if [ `uname -s` = "Darwin" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home)
else
    [ -z $JAVA_HOME ] && . $I2PBASE/core/c/find-java-home
fi
if [ ! -f "$JAVA_HOME/include/jni.h" ]; then
    echo "Cannot find jni.h! Looked in '$JAVA_HOME/include/jni.h'"
    echo "Please set JAVA_HOME to a java home that has the JNI"
    exit 1
fi

#
# API level, pulled from client build.gradle
#
LEVEL=$(awk -F' ' '/minSdkVersion/{print $2}' ../../../build.gradle)

#
#
# NDK r28+ uses the LLVM/Clang toolchain exclusively.
# GCC was removed in NDK r18. Standalone toolchains were removed in NDK r19.
# We now use the NDK's built-in clang directly.
#
# 16KB page alignment: All LOAD segments in .so files must be aligned to
# at least 16KB (0x4000) for Google Play Store compliance.
# This is achieved via: -Wl,-z,max-page-size=16384
#

TOOLCHAIN_DIR="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
if [ ! -d "$TOOLCHAIN_DIR" ]; then
    # Try macOS path
    TOOLCHAIN_DIR="$NDK/toolchains/llvm/prebuilt/darwin-x86_64"
fi
if [ ! -d "$TOOLCHAIN_DIR" ]; then
    echo "Cannot find NDK toolchain in $NDK/toolchains/llvm/prebuilt/"
    exit 1
fi

for ABI in "armeabi-v7a" "arm64-v8a"; do

# ABI-specific settings
case "$ABI" in
    "armeabi-v7a")
        ARCH="arm"
        TARGET="armv7a-linux-androideabi"
        export BINPREFIX="arm-linux-androideabi-"
        CONFIGUREHOST="arm-linux-androideabi"
        ;;
    "arm64-v8a")
        ARCH="aarch64"
        TARGET="aarch64-linux-android"
        export BINPREFIX="aarch64-linux-android-"
        CONFIGUREHOST="aarch64-linux-android"
        ;;
esac

if [ ! -e $PWD/$ABI ]
then
    mkdir $PWD/$ABI
fi

LIBFILE=$PWD/$ABI/libjbigi.so
if [ -f $LIBFILE ]
then
    echo "$LIBFILE exists, nothing to do here"
    echo "If you wish to force a recompile, delete it"
    continue
fi

if [ `uname -m` = "x86_64" ]; then
    BUILDHOST="x86_64-pc-linux-gnu"
else
    BUILDHOST="x86-pc-linux-gnu"
fi
if [ `uname -s` = "Darwin" ]; then
    BUILDHOST="x86-darwin"
fi

# NDK r19+ uses a unified toolchain - no standalone toolchain needed
COMPILER="$TOOLCHAIN_DIR/bin/${TARGET}${LEVEL}-clang"
if [ ! -f "$COMPILER" ]; then
    echo "Cannot find compiler $COMPILER"
    echo "Check that NDK version supports API level $LEVEL for target $TARGET"
    exit 1
fi
export CC="$COMPILER"
export NM="$TOOLCHAIN_DIR/bin/llvm-nm"
STRIP="$TOOLCHAIN_DIR/bin/llvm-strip"

export LIBGMP_LDFLAGS='-avoid-version'

# Common 16KB page alignment flag - required for Google Play Store
PAGE_SIZE_FLAGS='-Wl,-z,max-page-size=16384'

case "$ARCH" in
    "arm")
        MPN_PATH="arm/v6t2 arm/v6 arm/v5 arm generic"
        BASE_CFLAGS='-O2 -g -pedantic -fomit-frame-pointer -ffunction-sections -funwind-tables -fstack-protector -fno-strict-aliasing'
        export CFLAGS="${BASE_CFLAGS} -march=armv7-a -mfloat-abi=softfp -mfpu=vfp"
        export LDFLAGS="-Wl,--fix-cortex-a8 -Wl,--no-undefined -Wl,-z,noexecstack -Wl,-z,relro -Wl,-z,now ${PAGE_SIZE_FLAGS}"
        ;;
    "aarch64")
        MPN_PATH="arm64 generic"
        BASE_CFLAGS='-O2 -g -pedantic -fomit-frame-pointer -ffunction-sections -funwind-tables -fstack-protector -fno-strict-aliasing'
        export CFLAGS="${BASE_CFLAGS}"
        export LDFLAGS="-Wl,--no-undefined -Wl,-z,noexecstack -Wl,-z,relro -Wl,-z,now ${PAGE_SIZE_FLAGS}"
        ;;
esac

#echo "CC is $CC"

mkdir -p build
cd build

# we must set both build and host, so that the configure
# script will set cross_compile=yes, so that it
# won't attempt to run the a.out files
if [ ! -f config.status ]; then
    echo "Configuring GMP..."
    if [ -z "$MPN_PATH" ]; then
        $GMP/configure --with-pic --build=$BUILDHOST --host=$CONFIGUREHOST || exit 1
    else
        $GMP/configure --with-pic --build=$BUILDHOST --host=$CONFIGUREHOST MPN_PATH="$MPN_PATH" || exit 1
    fi
fi

echo "Building GMP..."
make -j8 || exit 1

COMPILEFLAGS="-fPIC -Wall $CFLAGS"
INCLUDES="-I. -I$JBIGI/jbigi/include -I$JAVA_HOME/include -I$JAVA_HOME/include/linux"
LINKFLAGS="-shared -Wl,-soname,libjbigi.so $LDFLAGS"

echo "Building jbigi lib that is statically linked to GMP"
STATICLIBS=".libs/libgmp.a"

echo "Compiling C code..."
rm -f jbigi.o $LIBFILE
echo "$CC -c $COMPILEFLAGS $INCLUDES $JBIGI/jbigi/src/jbigi.c"
$CC -c $COMPILEFLAGS $INCLUDES $JBIGI/jbigi/src/jbigi.c || exit 1
echo "$CC $LINKFLAGS $INCLUDES -o $LIBFILE jbigi.o $STATICLIBS"
$CC $LINKFLAGS $INCLUDES -o $LIBFILE jbigi.o $STATICLIBS || exit 1
echo "$STRIP $LIBFILE"
$STRIP $LIBFILE || exit 1

ls -l $LIBFILE || exit 1

cd ..
rm -r build

echo 'Built successfully'

done
