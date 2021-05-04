# Release Process

Note to all future maintainers: We have 4 channels that need to be updated in order to have a successful
Android release. Many of these channels are updated at different rates, and at times you must wait on a
third-party service to complete it's tasks before you may continue. When completing an Android release,
keep in mind that you must update 1) Maven 2) Google Play 3) f-droid.i2p.io and 4) F-Droid main
repository.

At the time of this revision, 2020/09/13, the main Android maintainer is idk. idk updates Maven, Google
Play, and f-droid.i2p.io, and nextl00p handles working with the F-Droid project to provide an I2P release
in their main repository.

## Docker

Docker users can use a shortcut to get an acceptable environment set up for building I2P for Android.

``` bash
docker build -t i2p.android.base .
docker run -it --rm --name i2p.android.base -v $(pwd)/app/build:/opt/workspace/i2p.android.base/app/build i2p.android.base
```

## Prerequirements

 1. Ensure you got the deprecated maven ant tasks. ( https://maven.apache.org/ant-tasks/download.cgi )
 2. It should exist at `~/.ant/lib/maven-ant-tasks-2.1.3.jar`
 3. Ensure you got hamcrest-integration, hamcrest-library, hamcrest-core in the hamcrest.home directory.
 4. Ensure junit 4.12 at least in junit.home, ensure the jar file is named `junit4.jar`.
 5. Ensure to have updated the changelog with the changes done.
 6. Ensure that you are configured to build i2p.i2p with Java 8. On Debian it is easiest to set with
   `update-java-alternatives --set java-8-openjdk-amd64` and picking Java 8. **TODO:** add instructions for non-Debian-based
   systems.
 7. Ensure that you have a Java 1.7 bootclasspath available. (See **Maven Central** step 2A.)

## Get all the dependencies ready

### Maven Central

 1. Check out a clean copy of i2p.i2p at the correct release version. (Make a clean checkout)
 2. Build the maven packages via `ant mavenCentral` where you end up with mavencentral-*.jar files in the 
  current directory.
 2. **A)** I2P for Android requires a Java 1.7 bootclasspath, but the servlet jar requires Java 8. So, to do the builds:
  - First set `javac.compilerargs=-bootclasspath /path/to/java/7/rt.jar:/path/to/java/7/jce.jar` in override.properties
  - Build with `ant mavenCentral`
  - upload everything *except* servlet.jar
  - Unset bootclasspath in override.properties
  - Build with `ant mavenCentral`
  - upload servlet.jar
 3. Login to http://oss.sonatype.org for uploading the mavencentral-*.jar bundles.
 4. In nexus, choose "Staging Upload" and upload all of the files with upload mode set to "Artifacts with POM". 
  When uploading the files to nexus, you *must* upload the pom.xml files, and all of their artifacts. For each 
  component, you will need to upload a *.jar, a *.jar.asc, a *sources.jar, a *sources.jar.asc, a javadoc.jar, 
  and a javadoc.jar.asc, and a pom.xml and a pom.xml.asc from the pkg-mavencentral directory during the "Upload
  Artifacts with POM" operation. You will need to do this once for each component you upload to Nexus.
 5. Under "Staging Repositories" ensure all where uploaded correctly, select them all and press "Release"
  in the toolbar.

#### Example override.properties:

        javac.version=1.7
        javac.target=1.7
        javac.source=1.8
        javac.compilerargs=-bootclasspath /home/user/StudioProjects/java7bootclasspath/rt.jar:/home/user/StudioProjects/java7bootclasspath/jce.jar
        javac.compilerargs7=-bootclasspath /home/user/StudioProjects/java7bootclasspath/rt.jar:/home/user/StudioProjects/java7bootclasspath/jce.jar
        build.built-by=name

### Android Common Build

 1. Edit `routerjars/local.properties` to use the clean i2p.i2p copy.
 2. Pull the latest translations with `tx pull -a` and commit them. (If you don't have the `tx` command,
  do `pip install transifex-client` )
  - If there are any new translations, `mtn add` them, and add them to `app/src/main/res/values/arrays.xml`
  (two places, alphabetical order please)
 3. Ensure that `signing.properties` contains the details of the release key.
 4. Edit `gradle.properties` to bump the I2P version.
 5. Edit `app/build.gradle` to bump the Android version number.
 6. Edit `CHANGELOG` to add the release and date.
 7. If the helper has changed since the last release, edit
    `lib/helper/gradle.properties` to bump the version.
 8. `./gradlew clean assembleRelease`

### Libraries

 1. `./gradlew :lib:client:uploadArchives`
 2. If the helper version was changed and should be released: `./gradlew :lib:helper:uploadArchives`
 3. Check on Sonatype that everything worked, and close/release.

## Release Packages

### F-Droid Guide

This guide is for f-droid.i2p.io, not for F-Droid's main repository. The repository keystore **and** the
config.py used to generate the repository are required to complete this process successfully.

 1. Ensure you have the release keys, the keyfile must be placed at `~/.local/share/fdroidserver/keystore.jks`
 2. If it's the first time, or you have reinstalled anything, ensure `path/to/fdroid/config.py` has correct
  information.
 3. Assuming you already have ran `./gradlew clean assembleRelease` from a earlier step, continue.
 4. `cp app/build/outputs/apk/free/release/app-free-release.apk path/to/fdroid/repo/I2P-VERSION.apk`
 5. Update `path/to/fdroid/metadata/net.i2p.android.txt` (The versions at the bottom of the file)
 6. Run `fdroid update` from inside the fdroid path (install fdroid command via `pip install fdroidserver`)
 7. Zip/tar the local fdroid repo and archive. `rm fdroid.tgz && tar czf fdroid.tgz archive/ repo/` from the
  fdroid directory.
 8. Push to download server and put in place. (via SSH for example, `scp fdroid.tgz download.i2p2.de:~/`)
 9. On the server run `bin-fd/update-fdroid` and `sudo bin-fd/update-app i2p 0.9.40` (This ensures we use the
  exact same apk file for the download page as in fdroid and gplay)
 10. Check F-Droid repo works, and app works.

### Google Play and finishing up

 1. Verify which files that are changed via `mtn ls cha`. It shouldn't be much more than those bellow this
  line and possible translations (`mtn ls unk`).
 2. Commit your release changes, `mtn ci gradle.properties lib/helper/gradle.properties app/build.gradle`
 3. Push free and donate builds to Google Play via https://play.google.com/apps/publish/
 4. Tag the new release. Example `mtn tag h: android-0.9.36`
 5. Push the monotone changes. Make sure that they are there at the next git sync.
 6. Update download page (version and hash, including F-Droid)

