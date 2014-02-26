These instructions are for a recent Android SDK (Rev 20 or better) on Linux.
Windows building is not currently supported.

These instructions were last updated for SDK Tools Version 20 with
SDK Platform-tools Version 12 from updates.

The i2p source must be installed in ../i2p.i2p,
or else add i2psrc=/path/to/source in the local.properties file.

=====================

Dependencies:

- Java SDK (preferably Oracle/Sun or OpenJDK) 1.6.0 or higher
- Apache Ant 1.8.0 or higher
- I2P source in ../i2p.i2p
- Android SDK (tested with Rev 22.3 and platform-tools version 19)

=====================

Instructions:

# Download the SDK from http://developer.android.com/sdk/index.html
# Unzip the android SDK in ../
# So then the android tools will be in ../android-sdk-linux/tools/
#
# Run the GUI updater, which you must do to get an SDK Platform:
../android-sdk-linux/tools/android &

# now go to the available packages tab, check the box and click refresh,
# and download an SDK Platform
# Since I2P is targeted at 4.4 (API 19)
# download at least that one. Otherwise you must change the
# target in project.properties from android-19 to andriod-x
# where x is the API version.

# I2P is configured to run on 2.2 (API 8) or higher using the
# Android Support Library, so download that as well
# (it's under "Extras"). 

# update the compatibility project
../android-sdk-linux/tools/android update lib-project -p ../android-sdk-linux/extras/android/support/v7/appcompat -t android-19

# To run the debugger (ddms) you also need to download the
# "Android SDK Platform-Tools" package from the GUI updater.

# create a file local.properties with the following line (without the leading # of course),
# do NOT use a relative path
# sdk.dir=/path/to/your/android-sdk-linux
# Copy this file to the routerjars/ directory, it is needed in both places.

# If your SDK is not in ../android-sdk-linux/ then you must
# override the location of the Android Support Library. Add
# the following line to local.properties
# do NOT use an absolute path
# android.library.reference.2=path/to/your/android-sdk-linux/extras/android/support/v7/appcompat
# Don't add it to the local.properties in the routerjars/ directory.

# DO NOT create a new project or anything. It's all set up right here for you.

# Create the android 4.4 (API 19) virtual device
# (don't make a custom hardware profile)
../android-sdk-linux/tools/android create avd --name i2p --target android-19

# then run the emulator:
#  This may take a LONG time the first time (half an hour or more)...
#  Run the debugger to ensure it is making progress
#   -no-boot-anim for faster boot
#   -dns-server 8.8.8.8 if the router can't reseed
#     ../android-sdk-linux/tools/emulator -avd i2p -no-boot-anim -dns-server 8.8.8.8 &
../android-sdk-linux/tools/emulator -avd i2p &

# or to talk to a real device in debug mode:
# You have to do this if you get a permission error -
# Stop ddms, unplug the device, do the following,
# then plug in the device, then start ddms
adb kill-server
sudo adb start-server
adb devices

# then wait a couple minutes until the emulator or device is up
# compile and install for a release
ant release
ant installr

# or compile and install for a debug version
ant debug
ant installd

# then run the debugger
../android-sdk-linux/tools/ddms &

# to rebuild and reinstall to emulator or device:
ant clean
# then do which ever from the above compile and install choices.


# to uninstall
ant uninstall
# or use your device's menu.

# Other ant tagets are available, just type
ant

# Anyway, with I2P installed, click on the I2P icon on your device and enjoy!

#other helpful commands
../android-sdk-linux/platform-tools/adb shell
../android-sdk-linux/platform-tools/adb pull /some/file/on/emulator some-local-dir/

# copy the Dev Tools app from the emulator to your device
adb -e pull /system/app/Development.apk ./Development.apk
adb -d install Development.apk

# reinstall an existing apk onto the emulator
adb -e install -r bin/I2PAndroid-debug.apk
