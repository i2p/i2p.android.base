FROM menny/android_ndk
ENV VERSION=0.9.50
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
RUN echo 'deb https://deb.i2p2.de/ sid main' >> /etc/apt/sources.list
RUN echo 'deb-src https://deb.i2p2.de/ sid main' >> /etc/apt/sources.list
RUN echo 'deb http://archive.ubuntu.com/ubuntu trusty universe' >> /etc/apt/sources.list
RUN wget -O /etc/apt/trusted.gpg.d/i2p-debian-repo.key.asc https://geti2p.net/_static/i2p-debian-repo.key.asc
COPY etc/debian-jessie-repo.key.asc /etc/apt/trusted.gpg.d
RUN mkdir -p /opt/packages && wget -O /opt/packages/openjdk-7-jre-headless.deb http://security.debian.org/debian-security/pool/updates/main/o/openjdk-7/openjdk-7-jre-headless_7u261-2.6.22-1~deb8u1_amd64.deb
RUN apt-get update
RUN apt-get build-dep -y i2p i2p-router
RUN apt-get install -y ant openjdk-8* libxml2-utils junit4 libhamcrest-java libmockito-java libmaven-ant-tasks-java dpkg-sig maven
RUN cd /opt/packages && dpkg-sig -l openjdk-7-jre-headless.deb && dpkg -x openjdk-7-jre-headless.deb /opt/packages/openjdk-7-jre
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
COPY etc/docker.signing.properties /opt/workspace/i2p.android.base/signing.properties
WORKDIR /opt/workspace/i2p.android.base
RUN find /opt/android-sdk-linux -type d -print0 | xargs -0 chown -R 1000:1000
RUN find /opt/android-sdk-linux -type d -print0 | xargs -0 chmod -Rc o+rw
RUN find /opt/android-sdk-linux -type d -print0 | xargs -0 chmod -c 0755
RUN find /opt/workspace -type d -print0 | xargs -0 chown -R 1000:1000
RUN find /opt/workspace -type d -print0 | xargs -0 chmod -Rc o+rw
RUN find /opt/workspace -type d -print0 | xargs -0 chmod -c 0755
CMD cd /opt/workspace/i2p.i2p && \
	ant -k mavenCentral; \
	cp -v *.jar pkg-mavencentral/; \
	cd /opt/workspace/i2p.android.base && \
	./gradlew --continue dependencies || true ; \
	 ./gradlew --continue assembleRelease
