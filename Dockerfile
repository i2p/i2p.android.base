FROM menny/android_ndk
ENV VERSION=0.9.49
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
#ENV PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/android-sdk-linux/cmdline-tools/latest/bin:/opt/android-sdk-linux/platform-tools
RUN echo 'deb https://deb.i2p2.de/ sid main' >> /etc/apt/sources.list
RUN echo 'deb-src https://deb.i2p2.de/ sid main' >> /etc/apt/sources.list
RUN echo 'deb http://archive.ubuntu.com/ubuntu trusty universe' >> /etc/apt/sources.list
RUN wget -O /etc/apt/trusted.gpg.d/i2p-debian-repo.key.asc https://geti2p.net/_static/i2p-debian-repo.key.asc
RUN mkdir -p /opt/packages && \
	wget -O /opt/packages/openjdk-7-jre-headless.deb http://security.debian.org/debian-security/pool/updates/main/o/openjdk-7/openjdk-7-jre-headless_7u261-2.6.22-1~deb8u1_amd64.deb
RUN cd /opt/packages && dpkg -x openjdk-7-jre-headless.deb /opt/packages/openjdk-7-jre
RUN apt-get update
RUN apt-get build-dep -y i2p i2p-router
RUN apt-get install -y ant openjdk-8* libxml2-utils monotone junit4 libhamcrest-java libmockito-java libmaven-ant-tasks-java
RUN git clone https://i2pgit.org/i2p-hackers/i2p.i2p --depth=1 -b i2p-$VERSION /opt/workspace/i2p.i2p
RUN update-java-alternatives --jre-headless --set java-1.8.0-openjdk-amd64
RUN update-java-alternatives --set java-1.8.0-openjdk-amd64
RUN update-alternatives --set javac /usr/lib/jvm/java-8-openjdk-amd64/bin/javac
RUN update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
RUN rm /opt/java/openjdk/ -rfv
COPY . /opt/workspace/i2p.android.base
COPY etc/docker.local.ndk.properties /opt/workspace/i2p.android.base/client/local.properties
COPY etc/docker.local.router.properties /opt/workspace/i2p.android.base/routerjars/local.properties
COPY etc/docker.local.sdk.properties /opt/workspace/i2p.android.base/local.properties
COPY etc/docker.override.properties /opt/workspace/i2p.android.base/override.properties
COPY etc/docker.override.properties /opt/workspace/i2p.i2p/override.properties
WORKDIR /opt/workspace/i2p.android.base
CMD cd /opt/workspace/i2p.i2p && ant mavenCentral; cp -v mavencentral*.jar pkg-temp-copy/ ; cp -rv pkg-mavencentral/* pkg-temp-copy/; cd /opt/workspace/i2p.android.base && ./gradlew dependencies || true ; ./gradlew assembleRelease
