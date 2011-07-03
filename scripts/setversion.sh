#
#  Get the version number and fix up AndroidManifest.xml
#  Public domain
#
THISDIR=$(realpath $(dirname $(which $0)))
cd $THISDIR
MANIFEST=../AndroidManifest.xml
TMP=AndroidManifest.xml.tmp
I2PBASE=${1:-../../i2p.i2p}

CORE=`grep 'public final static String VERSION' $I2PBASE/core/java/src/net/i2p/CoreVersion.java | \
         cut -d '"' -f 2`

MAJOR=`echo $CORE | cut -d '.' -f 1`
MINOR=`echo $CORE | cut -d '.' -f 2`
RELEASE=`echo $CORE | cut -d '.' -f 3`

ROUTERBUILD=$((`grep 'public final static long BUILD' $I2PBASE/router/java/src/net/i2p/router/RouterVersion.java | \
         cut -d '=' -f 2 | \
         cut -d ';' -f 1`))

ANDROIDBUILD=`grep 'build.number' build.number | \
         cut -d '=' -f 2`

SDK=`grep 'android:minSdkVersion' $MANIFEST | \
         cut -d '"' -f 2`

# don't let build number get too long
VERSIONSTRING="${CORE}-${ROUTERBUILD}_b$(($ANDROIDBUILD % 512))-API$SDK"

#
# Android version code is an integer.
# So we have 31 bits.
# MAJOR	 	4 bits 0-15
# MINOR 	8 bits 0-255
# RELEASE	8 bits 0-255
# ROUTERBUILD	7 bits 0-127
# ANDROIDBUILD	4 bits 0-15
#
# Note that ANDROIDBUILD is modded % 16, it will wrap,
# beware of that if you release multiple builds using the
# same ROUTERBUILD, or clear it if you update ROUTERBUILD
# Subtract 1 from ANDROIDBUILD since it starts at 1 after distclean.
#
VERSIONINT=$(( \
		(($MAJOR % 16) << 27) + \
		(($MINOR % 256) << 19) + \
		(($RELEASE % 256) << 11) + \
		(($ROUTERBUILD % 128) << 4) + \
		(($ANDROIDBUILD - 1) % 16) \
	      ))

echo "Android version: '$VERSIONSTRING' (${VERSIONINT})"
echo "my.version.name=${VERSIONSTRING}" > version.properties
echo "my.version.code=${VERSIONINT}" >> version.properties

SUBST='s/android.versionCode="[0-9]*"/android.versionCode="'${VERSIONINT}'"/'
sed "$SUBST" < $MANIFEST > $TMP
SUBST='s/android.versionName="[^"]*"/android.versionName="'${VERSIONSTRING}'"/'
sed "$SUBST" < $TMP > $MANIFEST
rm -f $TMP
