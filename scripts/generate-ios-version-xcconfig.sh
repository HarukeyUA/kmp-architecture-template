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

# This machine's address, baked into dev builds as the default server host so a freshly built dev
# app on a physical device reaches the server running here with zero manual setup. Prefer the
# Bonjour name — it survives DHCP lease changes and iOS resolves `.local` natively — over the
# numeric LAN IP. Empty when neither resolves (offline build); the app then falls back to
# localhost. Prod builds never read this (see Info-dev.plist). Host only, not a URL: `//` would
# start an xcconfig comment.
DEV_SERVER_HOST=${DEV_SERVER_HOST:-}
if [ -n "$DEV_SERVER_HOST" ]; then
    case "$DEV_SERVER_HOST" in
        *[!A-Za-z0-9.-]*)
            echo "error: DEV_SERVER_HOST must be a hostname or IPv4 address without a scheme or port" >&2
            exit 1
            ;;
    esac
fi
if [ -z "$DEV_SERVER_HOST" ] && command -v scutil >/dev/null 2>&1; then
    BONJOUR_NAME=$(scutil --get LocalHostName 2>/dev/null || true)
    [ -n "$BONJOUR_NAME" ] && DEV_SERVER_HOST="$BONJOUR_NAME.local"
fi
if [ -z "$DEV_SERVER_HOST" ]; then
    DEV_SERVER_HOST=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)
fi

mkdir -p "$(dirname "$OUTPUT")"
{
    printf 'CURRENT_PROJECT_VERSION=%s\n' "$BUILD_NUMBER"
    printf 'MARKETING_VERSION=%s\n' "$VERSION_NAME"
    printf 'DEV_SERVER_HOST=%s\n' "$DEV_SERVER_HOST"
} > "$OUTPUT"
