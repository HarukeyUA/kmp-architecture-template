#!/bin/sh
# One-command local dev stack: Postgres + MinIO (docker compose) + :server:app.
#
#   scripts/dev-stack.sh          # localhost mode: desktop / iOS simulator / Android emulator
#   scripts/dev-stack.sh --lan    # LAN mode: physical devices (anything not on this machine)
#   scripts/dev-stack.sh --down   # stop containers, keep data volumes
#   scripts/dev-stack.sh --nuke   # stop containers AND drop volumes (fresh DB + bucket)
#   DEV_SERVER_HOST=192.168.1.23 scripts/dev-stack.sh --lan   # explicit shared host
#
# The mode decides which host the server bakes into presigned blob URLs (SigV4 signs the host, so
# it cannot be rewritten client-side): localhost mode mints localhost:9000 — right for clients on
# this machine, unreachable from emulators and devices; LAN mode mints <lan-ip>:9000 — reachable
# from everything on the network. The failure smell is always "everything works except blobs":
# you are in localhost mode, restart with --lan.
set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$REPO_ROOT"

usage() {
    sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
}

MODE=localhost
case "${1:-}" in
    "") ;;
    --lan) MODE=lan ;;
    --down)
        docker compose down
        echo "Containers stopped. Data volumes kept — --nuke drops them."
        exit 0
        ;;
    --nuke)
        docker compose down --volumes
        echo "Containers stopped and volumes dropped: the next boot starts with a fresh DB and"
        echo "bucket. Sessions live in the DB, so running clients see auth.session_expired on"
        echo "their next refresh and bounce to Login — the designed recovery path."
        exit 0
        ;;
    -h | --help)
        usage
        exit 0
        ;;
    *)
        usage >&2
        exit 1
        ;;
esac

if [ "$MODE" = "lan" ]; then
    LAN_HOST=${DEV_SERVER_HOST:-}
    if [ -n "$LAN_HOST" ]; then
        case "$LAN_HOST" in
            *[!A-Za-z0-9.-]*)
                echo "error: DEV_SERVER_HOST must be a hostname or IPv4 address without a scheme or port" >&2
                exit 1
                ;;
        esac
    fi
    [ -n "$LAN_HOST" ] || LAN_HOST=$(ipconfig getifaddr en0 2>/dev/null || true)
    [ -n "$LAN_HOST" ] || LAN_HOST=$(ipconfig getifaddr en1 2>/dev/null || true)
    [ -n "$LAN_HOST" ] || LAN_HOST=$(hostname -I 2>/dev/null | awk '{print $1}' || true)
    if [ -z "$LAN_HOST" ]; then
        echo "error: could not determine a LAN IP (tried en0, en1, hostname -I)" >&2
        echo "Set DEV_SERVER_HOST explicitly and retry." >&2
        exit 1
    fi
fi

# Reports the listener occupying a TCP port, empty if the port is free.
port_holder() {
    lsof -nP -iTCP:"$1" -sTCP:LISTEN 2>/dev/null | awk 'NR==2 {print $1 " (pid " $2 ")"}'
}

# Compose fails opaquely when its published ports are taken; name the squatter up front. Docker
# itself holding 5432/9000 is fine — that's our own (or a reconcilable) compose stack.
for port in 5432 9000 9001; do
    holder=$(port_holder "$port")
    case "$holder" in
        "" | [Dd]ocker* | com.docke* | [Oo]rb*) ;;
        *)
            echo "error: port $port is held by $holder — stop it or change docker-compose.yml" >&2
            exit 1
            ;;
    esac
done
for port in 8080 8081; do
    holder=$(port_holder "$port")
    if [ -n "$holder" ]; then
        echo "error: port $port (server/metrics) is held by $holder — is another dev stack already running?" >&2
        exit 1
    fi
done

# Local credentials are throwaway compose values, so a missing .env is auto-created, not an error.
if [ ! -f .env ]; then
    cp .env.example .env
    echo "No .env found — copied .env.example to .env (gitignored, local compose values only)."
fi

# `--wait` counts the one-shot minio-init's clean exit as a failure, so start everything first,
# then wait for health only on the long-running services.
docker compose up -d
docker compose up -d --wait postgres minio

# ServerConfig reads process env only. Exporting .env here is the whole trick — these are the
# compose-local values; real deployment secrets are never read by this script.
set -a
# shellcheck disable=SC1091
. ./.env
set +a

# Local runs are intentionally more diagnostic than deployed servers. Override with INFO when a
# quieter session is useful; production does not use this script and logback defaults to INFO.
export LOG_LEVEL="${LOG_LEVEL:-DEBUG}"

if [ "$MODE" = "lan" ]; then
    # Must be exported before the server starts: every presigned URL it mints embeds this host.
    export S3_ENDPOINT="http://$LAN_HOST:9000"

    cat <<EOF

============================== dev stack: LAN mode ==============================
 server   http://$LAN_HOST:8080   (Android emulator: http://10.0.2.2:8080 also works)
 metrics  http://localhost:8081/metrics
 minio    http://$LAN_HOST:9000   (presigned host — baked into URLs the server mints)
 console  http://localhost:9001   (minio / minio12345)
 logs     $LOG_LEVEL (override with LOG_LEVEL=INFO)

 Deterministic client builds (especially on VPN/multi-NIC machines):
   DEV_SERVER_HOST=$LAN_HOST ./gradlew :client:androidApp:installDevDebug
   DEV_SERVER_HOST=$LAN_HOST xcodebuild ... -scheme iosApp-dev

 Without the variable, Android auto-detects a LAN IP and iOS bakes its Bonjour
 name. The host is a build-time snapshot — rebuild and reinstall to change it
 (the template has no runtime server override).

 First LAN run: macOS will ask to allow incoming connections for "java".
 Deny it and devices time out with no useful error.
=================================================================================
EOF
else
    cat <<EOF

=========================== dev stack: localhost mode ===========================
 server   http://localhost:8080   (Android emulator: http://10.0.2.2:8080)
 metrics  http://localhost:8081/metrics
 minio    http://localhost:9000   (presigned host — this machine only!)
 console  http://localhost:9001   (minio / minio12345)
 logs     $LOG_LEVEL (override with LOG_LEVEL=INFO)

 Clients — dev builds point here out of the box:
   ./gradlew -PappEnv=dev :client:desktopApp:run
   ./gradlew :client:androidApp:installDevDebug   # emulator reaches the host via 10.0.2.2
   iosApp via Xcode, scheme iosApp-dev            # simulator shares the host's localhost

 Presigned blob URLs say localhost:9000, which emulators and devices cannot
 reach — "everything works except blobs" means restart with --lan.
=================================================================================
EOF
fi

exec ./gradlew :server:app:run
