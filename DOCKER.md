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

Copy the `etc/docker.signing.example.proprties` file to `etc/docker.signing.proprties`,
edit it to match your key information and rebuild the container.

Run:

        docker run -it \
          -u $(id -u):$(id -g) \
          --name i2p.android.base \
          -v $HOME/.gnupg/:/.gnupg/:ro \
          -v $HOME/.i2p-plugin-keys/:/.i2p-plugin-keys/:ro \
          -v /run/user/$(id -u)/:/run/user/$(id -u)/:ro \
          i2p.android.base

To get the build artifacts for uploading to Maven out of the container, use:

        docker cp i2p.android.base:/opt/workspace/i2p.i2p/pkg-mavencentral app/pkg-mavencentral
        docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-i2p.jar app/pkg-mavencentral
        docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-mstreaming.jar app/pkg-mavencentral
        docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-router.jar app/pkg-mavencentral
        docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-servlet-i2p.jar app/pkg-mavencentral
        docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-streaming.jar app/pkg-mavencentral

To get the Android build artifacts out of the container, use:

        docker cp i2p.android.base:/opt/workspace/i2p.android.base/app/build/ app/build

And your android applications will appear in the `app/build` directory, in the same
place where non-container builds would go.

If you encounter a permissions error when rebuilding, delete the `app/build`,
`app/pkg-mavencentral` and `app/pkg-temp` path.

        rm -rf app/pkg-temp app/build app/pkg-mavencentral

Copypasta
---------

Once you have set up builds for the first time, from then on you can update the container and
build a fresh set of Maven jars and a new I2P for Android app by copy-pasting the following
commands:

``` sh
rm -rf app/pkg-temp app/build app/pkg-mavencentral
docker build -t i2p.android.base .
docker run -it \
  -u $(id -u):$(id -g) \
  --name i2p.android.base \
  -v $HOME/.gnupg/:/.gnupg/:ro \
  -v $HOME/.i2p-plugin-keys/:/.i2p-plugin-keys/:ro \
  -v /run/user/$(id -u)/:/run/user/$(id -u)/:ro \
  i2p.android.base
docker cp i2p.android.base:/opt/workspace/i2p.i2p/pkg-mavencentral app/pkg-mavencentral
docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-i2p.jar app/pkg-mavencentral
docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-mstreaming.jar app/pkg-mavencentral
docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-router.jar app/pkg-mavencentral
docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-servlet-i2p.jar app/pkg-mavencentral
docker cp i2p.android.base:/opt/workspace/i2p.i2p/mavencentral-streaming.jar app/pkg-mavencentral
docker cp i2p.android.base:/opt/workspace/i2p.android.base/app/build/ app/build
```
