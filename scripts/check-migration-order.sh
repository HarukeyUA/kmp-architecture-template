#!/usr/bin/env bash
set -euo pipefail

base_ref="${MIGRATION_BASE_REF:-origin/main}"

usage() {
    cat <<'USAGE'
Usage: scripts/check-migration-order.sh [--base <ref>]

Fails when a newly added Flyway versioned migration has a version that is not
greater than the latest versioned migration on the base ref. This keeps
timestamp-versioned, per-domain migrations compatible with Flyway outOfOrder=false.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --base)
            [[ $# -ge 2 ]] || {
                echo "Missing value for --base" >&2
                exit 2
            }
            base_ref="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

repo_root="$(git rev-parse --show-toplevel 2>/dev/null)" || {
    echo "Not inside a git repository." >&2
    exit 2
}
cd "$repo_root"

if ! git rev-parse --verify --quiet "${base_ref}^{commit}" >/dev/null; then
    cat >&2 <<EOF
Base ref '$base_ref' is not available locally.
Fetch it first, or run with --base <ref>. In GitHub Actions, use actions/checkout
with fetch-depth: 0 for this check.
EOF
    exit 2
fi

migration_version_from_path() {
    local path="$1"
    local filename="${path##*/}"

    if [[ "$filename" =~ ^V([0-9]+)__.+\.sql$ ]]; then
        printf '%s\n' "${BASH_REMATCH[1]}"
    fi
}

added_migration_paths() {
    {
        git diff --name-only --diff-filter=A "$base_ref"...HEAD -- ':(glob)**/db/migration/V*.sql'
        git diff --name-only --cached --diff-filter=A -- ':(glob)**/db/migration/V*.sql'
        git diff --name-only --diff-filter=A -- ':(glob)**/db/migration/V*.sql'
        git ls-files --others --exclude-standard -- ':(glob)**/db/migration/V*.sql'
    } | sort -u
}

latest_base_version="$(
    git ls-tree -r --name-only "$base_ref" |
        while IFS= read -r path; do
            migration_version_from_path "$path"
        done |
        sort -n |
        tail -1
)"

if [[ -z "$latest_base_version" ]]; then
    echo "No base migrations found on $base_ref; migration order check passed."
    exit 0
fi

failed=0

while IFS= read -r path; do
    [[ -n "$path" ]] || continue

    version="$(migration_version_from_path "$path")"
    [[ -n "$version" ]] || continue

    if (( 10#$version <= 10#$latest_base_version )); then
        cat >&2 <<EOF
Migration timestamp is stale:
  $path
  version:      $version
  latest base:  $latest_base_version ($base_ref)

Refresh this migration's timestamp so strict Flyway ordering remains valid.
EOF
        failed=1
    fi
done < <(added_migration_paths)

if (( failed )); then
    exit 1
fi

echo "Migration order check passed against $base_ref."
