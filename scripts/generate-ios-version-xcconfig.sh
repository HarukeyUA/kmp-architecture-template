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

mkdir -p "$(dirname "$OUTPUT")"
printf 'CURRENT_PROJECT_VERSION=%s\n' "$BUILD_NUMBER" > "$OUTPUT"
