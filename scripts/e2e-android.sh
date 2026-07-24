#!/bin/sh
# Runs the Android E2E suite against a locally running dev stack.
#
#   scripts/dev-stack.sh          # first, in another terminal (localhost mode is enough)
#   scripts/e2e-android.sh        # then this: health check, run
#
# With several devices attached, select one with ANDROID_SERIAL=<serial>.
#
# What it does:
#  - checks http://localhost:8080/health so a dead stack fails here, not 15s into a test
#  - runs the suite with DEV_SERVER_HOST=10.0.2.2 (the emulator's native NAT alias for the
#    host's loopback) so the baked base URL is deterministic. For a physical device, start the
#    stack with `scripts/dev-stack.sh --lan` and export DEV_SERVER_HOST=<LAN IP> first — an
#    explicit value is respected.
#
# Animations (the top instrumented-flakiness source) need no manual step: `animationsDisabled`
# in androidApp's testOptions has `am instrument` zero all three scales for the run and restore
# the device's previous values afterwards.
set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$REPO_ROOT"

if ! curl -fsS --max-time 2 http://localhost:8080/health >/dev/null 2>&1; then
    echo "error: dev server not reachable at http://localhost:8080/health" >&2
    echo "Start it first: scripts/dev-stack.sh (localhost mode is enough for the E2E suite)." >&2
    exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
    echo "error: adb not found on PATH" >&2
    exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
    echo "error: no device/emulator visible to adb (or several without ANDROID_SERIAL set)" >&2
    exit 1
fi

DEV_SERVER_HOST="${DEV_SERVER_HOST:-10.0.2.2}" ./gradlew :client:androidApp:connectedDevDebugAndroidTest
