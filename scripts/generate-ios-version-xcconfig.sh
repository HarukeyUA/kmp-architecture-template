#!/bin/sh
set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
OUTPUT="$REPO_ROOT/iosApp/Configuration/Generated.xcconfig"

if command -v git >/dev/null 2>&1 && git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    BUILD_NUMBER=$(git -C "$REPO_ROOT" rev-list --count HEAD)
else
    BUILD_NUMBER=1
fi

VERSION_NAME=$(awk -F= '/^versionName=/ {print $2; exit}' "$REPO_ROOT/version.properties" | tr -d '[:space:]')
if [ -z "$VERSION_NAME" ]; then
    echo "error: versionName missing from version.properties" >&2
    exit 1
fi

mkdir -p "$(dirname "$OUTPUT")"
{
    printf 'CURRENT_PROJECT_VERSION=%s\n' "$BUILD_NUMBER"
    printf 'MARKETING_VERSION=%s\n' "$VERSION_NAME"
} > "$OUTPUT"
