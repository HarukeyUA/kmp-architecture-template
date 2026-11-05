#!/bin/sh
# Builds, installs and launches the iOS app on a connected iPhone from the terminal
#
#   scripts/run-ios-device.sh          # dev flavor (scheme iosApp-dev)
#   scripts/run-ios-device.sh prod     # prod flavor (scheme iosApp)
#
# With several iPhones plugged in, select one with IOS_DEVICE_ID=<UDID>
# (`xcrun devicectl list devices` prints them).
#
# Why this exists: the Android Studio KMP run action can skip the Gradle step that copies
# compose resources into the bundle, which crashes the app at startup with
# MissingResourceException. Here xcodebuild runs the Gradle script phase itself, so the bundle
# is always complete. Uses whatever `xcode-select -p` points at.
set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$REPO_ROOT"

case "${1:-dev}" in
    dev)
        SCHEME=iosApp-dev
        CONFIGURATION=Debug-dev
        BUNDLE_ID=org.example.project.dev
        ;;
    prod)
        SCHEME=iosApp
        CONFIGURATION=Debug
        BUNDLE_ID=org.example.project
        ;;
    *)
        echo "usage: $0 [dev|prod]" >&2
        exit 2
        ;;
esac

if [ -z "${IOS_DEVICE_ID:-}" ]; then
    DEVICES_JSON=$(mktemp)
    xcrun devicectl list devices --json-output "$DEVICES_JSON" >/dev/null
    IOS_DEVICE_ID=$(python3 - "$DEVICES_JSON" <<'EOF'
import json, sys
devices = json.load(open(sys.argv[1]))["result"]["devices"]
connected = [
    d["hardwareProperties"]["udid"]
    for d in devices
    if d["hardwareProperties"].get("reality") == "physical"
    and d["connectionProperties"].get("tunnelState") == "connected"
]
print(connected[0] if len(connected) == 1 else "")
EOF
    )
    rm -f "$DEVICES_JSON"
    if [ -z "$IOS_DEVICE_ID" ]; then
        echo "error: expected exactly one connected iPhone; set IOS_DEVICE_ID=<UDID> to choose" >&2
        xcrun devicectl list devices >&2
        exit 1
    fi
fi

DERIVED_DATA="$REPO_ROOT/iosApp/build/DerivedData"
APP="$DERIVED_DATA/Build/Products/$CONFIGURATION-iphoneos/Template.app"

xcodebuild -project iosApp/iosApp.xcodeproj -scheme "$SCHEME" -configuration "$CONFIGURATION" \
    -destination "id=$IOS_DEVICE_ID" -destination-timeout 15 -derivedDataPath "$DERIVED_DATA" build

if [ ! -d "$APP/compose-resources" ]; then
    echo "error: $APP has no compose-resources folder; the Gradle resource sync did not run" >&2
    exit 1
fi

xcrun devicectl device install app --device "$IOS_DEVICE_ID" "$APP"
xcrun devicectl device process launch --device "$IOS_DEVICE_ID" "$BUNDLE_ID"
