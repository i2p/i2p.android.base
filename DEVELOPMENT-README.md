# Development Readme

## How to build development builds of the router core for android?

Check the RELEASE-PROCESS.md file for general information about how to build and to bump the version.

In your i2p.i2p codebase checkout, execute `./installer/resources/maven-dev-release.sh` with your build number as first argument.
The script locates itself and uses the same codebase as it's in, to produce the maven builds which will be locally installed.

Next, add the build number to the gradle.properties and build the android build as usual.

