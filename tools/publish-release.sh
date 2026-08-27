#!/usr/bin/env bash
set -e

VERSION="${1:-v1.3.0.1}"
CLEAN_VER="${VERSION#v}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
CHANGELOG_PATH="$ROOT_DIR/CHANGELOG.md"
NOTES_EXTRACTOR="$SCRIPT_DIR/extract_release_notes.py"

cd "$ROOT_DIR"

echo "========================================================"
echo "🚀 LQD RELEASE WORKFLOW: $VERSION"
echo "========================================================"

# 1. Verify Release Notes in CHANGELOG.md
echo "📝 [1/5] Validating Release Notes in CHANGELOG.md..."
if [ ! -f "$CHANGELOG_PATH" ]; then
    echo "❌ Error: CHANGELOG.md missing!"
    exit 1
fi

RELEASE_NOTES=$(python3 "$NOTES_EXTRACTOR" "$VERSION" "$CHANGELOG_PATH")
if [ -z "$RELEASE_NOTES" ]; then
    echo "❌ Error: Release notes for $VERSION in CHANGELOG.md are empty!"
    exit 1
fi

echo "--- Release Notes Preview for $VERSION ---"
echo "$RELEASE_NOTES"
echo "-------------------------------------------"

# 2. Verify versionName in build.gradle.kts
echo "🔍 [2/5] Verifying versionName in app/build.gradle.kts..."
GRADLE_VER=$(grep 'versionName = "' app/build.gradle.kts | head -n 1 | sed -E 's/.*versionName = "([^"]+)".*/\1/')

if [ "$GRADLE_VER" != "$CLEAN_VER" ]; then
    echo "⚠️ Warning: app/build.gradle.kts has versionName='$GRADLE_VER', but release tag is '$CLEAN_VER'!"
    read -p "Do you want to continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Aborting release. Please sync app/build.gradle.kts with $VERSION."
        exit 1
    fi
fi

# 3. Run Unit Tests
echo "🧪 [3/5] Running Unit Tests..."
export JAVA_HOME=${JAVA_HOME:-/home/karthikeyan/android-dev/jdk-17}
export PATH=$JAVA_HOME/bin:$PATH
./gradlew testDebugUnitTest --quiet

# 4. Build Signed Release APK
echo "🔨 [4/5] Building Release APK..."
./gradlew assembleRelease

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: Release APK not found at $APK_PATH"
    exit 1
fi

# 5. Package & Publish to GitHub
echo "📦 [5/5] Publishing GitHub Release $VERSION..."
RELEASE_APK="LQD-${VERSION}.apk"
cp "$APK_PATH" "$RELEASE_APK"

REPO_NAME=$(gh repo view --json nameWithOwner -q .nameWithOwner)
FULL_NOTES="### LQD $VERSION Release

$RELEASE_NOTES

---
• **Direct APK Download**: [LQD-${VERSION}.apk](https://github.com/${REPO_NAME}/releases/download/${VERSION}/LQD-${VERSION}.apk)"

# Check if release already exists
if gh release view "$VERSION" >/dev/null 2>&1; then
    echo "🔄 Release $VERSION exists. Updating notes and uploading asset..."
    gh release edit "$VERSION" --title "LQD $VERSION" --notes "$FULL_NOTES" --latest --draft=false
    gh release upload "$VERSION" "$RELEASE_APK" --clobber
else
    echo "✨ Creating new release $VERSION..."
    gh release create "$VERSION" "$RELEASE_APK" \
        --title "LQD $VERSION" \
        --notes "$FULL_NOTES" \
        --latest
fi

rm -f "$RELEASE_APK"

echo ""
echo "========================================================"
echo "✅ LQD $VERSION PUBLISHED SUCCESSFULLY!"
echo "🌐 Release Link: https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/releases/tag/${VERSION}"
echo "📥 Download APK: https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/releases/latest/download/LQD-${VERSION}.apk"
echo "========================================================"
