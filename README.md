# I2P Android

## Build process

### Dependencies:

- Java SDK (preferably Oracle/Sun or OpenJDK) 1.6.0 or higher
- Apache Ant 1.8.0 or higher
- I2P source in ../i2p.i2p
- Android SDK (tested with Rev 22.6.4 and platform-tools version 19.1)
- Android Support Repository

### Preparation

1. Download the Android SDK. The simplest method is to download [Android Studio](https://developer.android.com/sdk/installing/studio.html).

  * If you are using an existing Android SDK, install the Android Support Repository via the SDK Manager.

2. Check out the [`i2p.i2p`](https://github.com/i2p/i2p.i2p) repository.

3. Create a `local.properties` file in `i2p.android.base/routerjars` containing:

    ```
    i2psrc=/path/to/i2p.i2p
    ```

4. Gradle will pull dependencies over the clearnet by default. To use Tor, create a `gradle.properties` file in `i2p.android.base` containing:

    ```
    systemProp.socksProxyHost=localhost
    systemProp.socksProxyPort=9150
    ```

### Building from the command line

1. Create a `local.properties` file in `i2p.android.base` containing:

    ```
    sdk.dir=/path/to/android-studio/sdk
    ```

2. `./gradlew assembleDebug`

3. The APK will be placed in `i2p.android.base/app/build/outputs/apk`.

### Building with Android Studio

1. Import `i2p.android.base` into Android Studio. (This creates the `local.properties` file automatically).

2. Build and run the app (`Shift+F10`).

### Commands from the old build instructions that might be useful

```
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

# Anyway, with I2P installed, click on the I2P icon on your device and enjoy!

#other helpful commands
../android-sdk-linux/platform-tools/adb shell
../android-sdk-linux/platform-tools/adb pull /some/file/on/emulator some-local-dir/

# copy the Dev Tools app from the emulator to your device
adb -e pull /system/app/Development.apk ./Development.apk
adb -d install Development.apk

# reinstall an existing apk onto the emulator
adb -e install -r bin/I2PAndroid-debug.apk
```
