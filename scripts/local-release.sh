#!/usr/bin/env bash
# A release APK built on this machine that installs over the published app: the stable key
# (SIGNING_PASSWORD) and a versionCode above the latest Release's (VERSION_CODE, or the latest
# published one + 1, read with gh). Checks the certificate like the CI's publish job does.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
[ -n "${SIGNING_PASSWORD:-}" ] || { echo "SIGNING_PASSWORD missing: the APK would not install over the published app" >&2; exit 1; }
if [ -z "${VERSION_CODE:-}" ]; then
    latest="$(gh release list --exclude-pre-releases --limit 1 --json tagName --jq '.[0].tagName' 2>/dev/null || true)"
    [[ "$latest" =~ ^v[0-9]+\.[0-9]+\.([0-9]+)$ ]] || { echo "VERSION_CODE missing and the latest release is unknown ('$latest'): set VERSION_CODE" >&2; exit 1; }
    VERSION_CODE=$((BASH_REMATCH[1] + 1))
    echo "VERSION_CODE=$VERSION_CODE (latest release: $latest)"
fi
export VERSION_CODE SIGNING_PASSWORD
./gradlew :app:assembleRelease ${GRADLE_ARGS:-}
./scripts/check-jni.sh
APK=app/build/outputs/apk/release/app-release.apk
EXPECTED="$(sed -n 's/^ *EXPECTED_CERT_SHA256: *\([0-9a-f]*\).*/\1/p' .github/workflows/ci.yml)"
APKSIGNER="$(ls -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"/build-tools/*/apksigner | sort -V | tail -1)"
"$APKSIGNER" verify --print-certs "$APK" | grep -q "SHA-256 digest: $EXPECTED" || { echo "FAIL: $APK is not signed with the stable key" >&2; exit 1; }
mkdir -p dist
cp "$APK" dist/pocket-studio.apk
echo "OK: dist/pocket-studio.apk (versionCode $VERSION_CODE, stable key)"
