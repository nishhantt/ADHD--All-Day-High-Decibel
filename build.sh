#!/usr/bin/env bash
GRADLE_TARGET=${1:-":app:assembleDebug"}

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed. Install Docker or use Android Studio to build."
  exit 1
fi

echo "Building with Docker (gradle:8.3-jdk17). This may take a few minutes..."
docker run --rm -v "$(pwd)":/home/gradle/project -w /home/gradle/project gradle:8.3-jdk17 gradle $GRADLE_TARGET
RC=$?
if [ $RC -ne 0 ]; then
  exit $RC
fi

echo "Build finished. APK (if produced) will be at app/build/outputs/apk/debug/app-debug.apk"
