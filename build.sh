#! /usr/bin/env sh
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
