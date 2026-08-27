#!/usr/bin/env bash
set -e

VERSION="${1:-v1.3.0}"
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
RELEASE_APK="LQD-${VERSION}.apk"
cp "$APK_PATH" "$RELEASE_APK"

echo "🚀 Creating GitHub Release $VERSION..."
gh release create "$VERSION" "$RELEASE_APK" \
    --title "LQD $VERSION" \
    --notes "### LQD $VERSION Release
- **Official LQD Rebranding**: High-fidelity vector-sharp app icon and modern startup screen.
- **UPI VPA Bank Disambiguation**: Resolved bug where beneficiary @vpa handles were mistaken for user bank accounts.
- **Stat Card Dynamic Typography**: Resolved horizontal number wrap on large expense values with adaptive scaling.
- **In-App Seamless OTA Updates**: Liquid titanium glass update modal with real-time download progress and zero emojis.
- **Download the attached APK below to install on your Android device.**"

rm "$RELEASE_APK"

echo "✅ Release $VERSION published successfully!"
echo "🌐 Shareable Download Link:"
echo "https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/releases/latest/download/LQD-${VERSION}.apk"
