#!/usr/bin/env bash
set -e

VERSION="${1:-v1.0.0}"
APK_PATH="app/build/outputs/apk/release/app-release.apk"

echo "🔨 Building Release APK..."
export JAVA_HOME=${JAVA_HOME:-/home/karthikeyan/android-dev/jdk-17}
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleRelease

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: Release APK not found at $APK_PATH"
    exit 1
fi

echo "📦 Preparing release asset..."
RELEASE_APK="Kaching-${VERSION}.apk"
cp "$APK_PATH" "$RELEASE_APK"

echo "🚀 Creating GitHub Release $VERSION..."
gh release create "$VERSION" "$RELEASE_APK" \
    --title "Kaching $VERSION" \
    --notes "### 💸 Kaching $VERSION
- Monotone 7-Day Cash Flow Insights & Interactive Trend
- Sleek Single-Strip Search & Filter Bottom Sheet
- Multi-Bank SMS Real-Time Reconciliation
- Download the attached APK below to install on your Android device."

rm "$RELEASE_APK"

echo "✅ Release $VERSION published successfully!"
echo "🌐 Shareable Download Link:"
echo "https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/releases/latest/download/Kaching-${VERSION}.apk"
