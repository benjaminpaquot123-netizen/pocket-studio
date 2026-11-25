#!/bin/bash
# Pocket Studio: Android tools ready in a cloud session (about 35 s). Never blocks a session.
# Run from the folder that holds the checkout: cd .. && bash pocket-studio/scripts/session-android.sh
# The same text is the setup script of the cloud environment « pocket-studio-android » (docs/AIDE-TELECHARGEMENTS.md).
set +e
SDK=/opt/android-sdk
SDKMANAGER=$SDK/cmdline-tools/latest/bin/sdkmanager
if [ ! -x "$SDKMANAGER" ]; then
  mkdir -p "$SDK/cmdline-tools" /tmp/ps-sdk
  curl -fsS --retry 5 --retry-all-errors -o /tmp/ps-sdk/tools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
  unzip -q -o /tmp/ps-sdk/tools.zip -d /tmp/ps-sdk
  rm -rf "$SDK/cmdline-tools/latest"
  mv /tmp/ps-sdk/cmdline-tools "$SDK/cmdline-tools/latest"
  rm -rf /tmp/ps-sdk
fi
export ANDROID_HOME=$SDK
yes | "$SDKMANAGER" --licenses > /dev/null 2>&1
# Same versions as the env block of .github/workflows/ci.yml.
for attempt in 1 2 3; do
  "$SDKMANAGER" "platforms;android-37.0" "build-tools;37.0.0" "platform-tools" \
    "ndk;30.0.16248370" "cmake;4.1.2" > /dev/null 2>&1 && break
  sleep 20
done
if [ -d pocket-studio ]; then
  echo "sdk.dir=$SDK" > pocket-studio/local.properties
  (cd pocket-studio && scripts/ci/robolectric-jar.sh)
fi
exit 0
