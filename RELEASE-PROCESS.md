1. Check out a clean copy of i2p.i2p at the correct release version.
2. Edit `routerjars/local.properties` to use the clean i2p.i2p copy.
3. Pull the latest translations with `tx pull -a` and commit them. (If you don't have the `tx` command, do `pip install transifex-client` )
4. Ensure that `signing.properties` contains the details of the release key.
5. Edit `gradle.properties` to bump the I2P version.
6. Edit `app/build.gradle` to bump the Android version number.
7. If the helper has changed since the last release, edit
   `lib/helper/gradle.properties` to bump the version.
8. `./gradlew clean assembleRelease`
9. `./gradlew :lib:client:uploadArchives`
10. If the helper version was changed: `./gradlew :lib:helper:uploadArchives`
11. Check on Sonatype that everything worked, and close/release.
12. Update local fdroidserver repo
13. `cp app/build/outputs/apk/free/release/app-free-release.apk path/to/fdroid/repo/I2P-VERSION.apk`
14. Update `path/to/fdroid/metadata/net.i2p.android.txt`
15. `fdroid update`
16. Push to download server and put in place.
17. Check F-Droid repo works, and app works.
18. `mtn ci gradle.properties lib/helper/gradle.properties app/build.gradle`
19. Push free and donate builds to Google Play.
