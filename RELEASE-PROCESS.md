# Release Process

## Prerequirements

1. Ensure you got the deprecated maven ant tasks. ( https://maven.apache.org/ant-tasks/download.cgi )
2. It should exist at `~/.ant/lib/maven-ant-tasks-2.1.3.jar`
3. Ensure you got hamcrest-integration, hamcrest-library, hamcrest-core in the hamcrest.home directory.
4. Ensure junit 4.12 at least in junit.home, ensure the jar file is named `junit4.jar`.

## Maven Central

1. Check out a clean copy of i2p.i2p at the correct release version. (Make a clean checkout)
2. Build the maven packages via `ant mavenCentral` where you end up with mavencentral-*.jar files in the current directory.
3. Login to http://oss.sonatype.org for uploading the mavencentral-*.jar bundles.
4. In nexus, choose "Staging Upload" and upload all of the bundles with upload mode set to "Artifact Bundle"
5. Under "Staging Repositories" ensure all where uploaded correctly, select them all and press "Release" in the toolbar.

## Android Common Build

1. Edit `routerjars/local.properties` to use the clean i2p.i2p copy.
2. Pull the latest translations with `tx pull -a` and commit them. (If you don't have the `tx` command, do `pip install transifex-client` )
3. Ensure that `signing.properties` contains the details of the release key.
4. Edit `gradle.properties` to bump the I2P version.
5. Edit `app/build.gradle` to bump the Android version number.
6. If the helper has changed since the last release, edit
   `lib/helper/gradle.properties` to bump the version.
7. `./gradlew clean assembleRelease`

### Steps to take if the android helper is updated and should be released

1. `./gradlew :lib:client:uploadArchives`
2. If the helper version was changed: `./gradlew :lib:helper:uploadArchives`
3. Check on Sonatype that everything worked, and close/release.

## F-Droid Guide

1. Ensure you have the release keys, the keyfile must be placed at `~/.local/share/fdroidserver/keystore.jks`
2. If it's the first time, or you have reinstalled anything, ensure `path/to/fdroid/config.py` has correct information.
3. Assuming you already have ran `./gradlew clean assembleRelease` from a earlier step, continue.
4. `cp app/build/outputs/apk/free/release/app-free-release.apk path/to/fdroid/repo/I2P-VERSION.apk`
5. Update `path/to/fdroid/metadata/net.i2p.android.txt` (The versions at the bottom of the file)
6. Run `fdroid update` from inside the fdroid path (install fdroid command via `pip install fdroidserver`)
7. Zip/tar the local fdroid repo and archive. `rm fdroid.tgz && tar czf fdroid.tgz archive/ repo/` from the fdroid directory.
8. Push to download server and put in place. (via SSH for example, `scp fdroid.tgz download.i2p2.de:~/`)
9. On the server run `bin-fd/update-fdroid` and `sudo bin-fd/update-app i2p 0.9.40` (This ensures we use the exact same apk file for the download page as in fdroid and gplay)
10. Check F-Droid repo works, and app works.

## Google Play and finishing up

1. Verify which files that are changed via `mtn ls cha`. It shouldn't be much more than those bellow this line and possible translations (`mtn ls unk`).
2. Commit your release changes, `mtn ci gradle.properties lib/helper/gradle.properties app/build.gradle`
3. Push free and donate builds to Google Play via https://play.google.com/apps/publish/
4. Tag the new release. Example `mtn tag h: android-0.9.36`
5. Push the monotone changes



