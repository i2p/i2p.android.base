#!/bin/bash
#
# build GMP and libjbigi.so using the Android tools directly
#
# TODO: Get more settings from environment variables set in ../custom-rules.xml
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
ROUTERJARS=$THISDIR/../../../../routerjars

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
GMPVER=4.3.2
GMP="$JBIGI/gmp-$GMPVER"

if [ ! -d "$GMP" ]; then
    echo "Source dir for GMP version $GMPVER not found in $GMP"
    echo "Install it there or change GMPVER and/or GMP in this script"
    exit 1
fi

#
# API level, pulled from ../AndroidManifest.xml
#
LEVEL=$(awk -F\" '/minSdkVersion/{print $2}' ../AndroidManifest.xml)

#
# 4.6 is the GCC version. GCC 4.4.3 support was removed in NDK r9b.
# Available in r10:
#	arm-linux-androideabi-4.6
#	arm-linux-androideabi-4.8
#	arm-linux-androideabi-clang3.3
#	arm-linux-androideabi-clang3.4
#	llvm-3.3
#	llvm-3.4
#	mipsel-linux-android-4.6
#	mipsel-linux-android-4.8
#	mipsel-linux-android-clang3.3
#	mipsel-linux-android-clang3.4
#	x86-4.6
#	x86-4.8
#	x86-clang3.3
#	x86-clang3.4
GCCVER=4.6

# Arch-specific settings
ARCH="arm"
ABIDIR="armeabi"
AABIPREFIX="arm-linux-androideabi-"
export BINPREFIX="arm-linux-androideabi-"
CONFIGUREHOST="armv5-eabi-linux"

LIBFILE=$PWD/$ABIDIR/libjbigi.so
if [ -f $LIBFILE ]
then
    echo "$LIBFILE exists, nothing to do here"
    echo "If you wish to force a recompile, delete it"
    exit 0
fi

export SYSROOT="$NDK/platforms/android-$LEVEL/arch-$ARCH/"
if [ ! -d "$SYSROOT" ]; then
    echo "Cannot find $SYSROOT in NDK, check for support of level: $LEVEL arch: $ARCH or adjust LEVEL and ARCH in script"
    exit 1
fi

export AABI="$AABIPREFIX$GCCVER"
if [ `uname -s` = "Darwin" ]; then
    export SYSTEM="darwin-x86"
elif [ `uname -m` = "x86_64" ]; then
    export SYSTEM="linux-x86_64"
else
    export SYSTEM="linux-x86"
fi

COMPILER="$NDK/toolchains/$AABI/prebuilt/$SYSTEM/bin/${BINPREFIX}gcc"
if [ ! -f "$COMPILER" ]; then
    echo "Cannot find compiler $COMPILER in NDK, check for support of system: $SYSTEM ABI: $AABI or adjust AABI and SYSTEM in script"
    exit 1
fi
export CC="$COMPILER --sysroot=$SYSROOT"
# worked without this on 4.3.2, but 5.0.2 couldn't find it
export NM="$NDK/toolchains/$AABI/prebuilt/$SYSTEM/bin/${BINPREFIX}nm"
STRIP="$NDK/toolchains/$AABI/prebuilt/$SYSTEM/bin/${BINPREFIX}strip"

#echo "CC is $CC"

mkdir -p build
cd build

# we must set both build and host, so that the configure
# script will set cross_compile=yes, so that it
# won't attempt to run the a.out files
if [ ! -f config.status ]; then
    echo "Configuring GMP..."
    if [ `uname -s` = "Darwin" ]; then
        $GMP/configure --with-pic --build=x86-darwin --host=$CONFIGUREHOST || exit 1
    else
        $GMP/configure --with-pic --build=x86-none-linux --host=$CONFIGUREHOST || exit 1
    fi
fi

echo "Building GMP..."
make || exit 1

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

COMPILEFLAGS="-fPIC -Wall"
INCLUDES="-I. -I$JBIGI/jbigi/include -I$JAVA_HOME/include -I$JAVA_HOME/include/linux"
LINKFLAGS="-shared -Wl,-soname,libjbigi.so,--fix-cortex-a8"

echo "Building jbigi lib that is statically linked to GMP"
STATICLIBS=".libs/libgmp.a"

echo "Compiling C code..."
rm -f jbigi.o $LIBFILE
echo "$CC -c $COMPILEFLAGS $INCLUDES $JBIGI/jbigi/src/jbigi.c"
$CC -c $COMPILEFLAGS $INCLUDES $JBIGI/jbigi/src/jbigi.c || exit 1
echo "$CC $LINKFLAGS $INCLUDES $INCLUDELIBS -o $LIBFILE jbigi.o $STATICLIBS"
$CC $LINKFLAGS $INCLUDES $INCLUDELIBS -o $LIBFILE jbigi.o $STATICLIBS || exit 1
echo "$STRIP $LIBFILE"
$STRIP $LIBFILE || exit 1

ls -l $LIBFILE || exit 1


echo 'Built successfully'
