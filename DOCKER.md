Docker Build Instructions
=========================

It is possible to build a container with a pre-installed environment for
correctly compiling an I2P for Android development build. Unlike the i2p.i2p
container, zero attempt has been made to optimize the size of the container,
as it contains a copy of the latest Android SDK, toolchains, and Android NDK,
which it must download. To save time, this is cached locally. It is likely to
take up to 30 GB of disk space to compile in this way, however, it is very easy
and convenient compared to the steps in RELEASE-PROCESS.md and may make
building Android reproducibly easier in the future.

Container dependencies
----------------------

  - `menny/android_ndk` (third-party image) (reviewed by idk) (depends on menny/android_sdk
  - `menny/android_sdk` (third-party image) (reviewed by idk) (depends on ubuntu/18.04)
  - `ubuntu/18.04` (official docker container) (base container)

Build the container locally
---------------------------

Run:

        docker build -t i2p.android.base .

To build the container. It will have a lot to download the first time, so it may take
a while to complete.

Run an Android build in the container
-------------------------------------

Run:

        docker run -it --rm --name i2p.android.base \
          -v $(pwd)/app/build:/opt/workspace/i2p.android.base/app/build \
          -v $(pwd)/app/pkg-temp:/opt/workspace/i2p.i2p/pkg-temp \
          i2p.android.base

And your android applications will appear in the `app/build` directory, in the same
place where non-container builds would go.

TODO: Containerized release builds