#!/usr/bin/env bash
#
# Derives the client-only variant of this template from the full-stack tree,
# in place. Run it in a fresh clone, a worktree, or a CI checkout — never in a
# working copy you care about:
#
#   git worktree add ../client-only-variant HEAD
#   cd ../client-only-variant && ./scripts/make-client-only.sh
#
# What it does:
#   1. Deletes the paths listed in scripts/client-only/strip-paths.txt
#      (the server, the shared seam, and everything client-side that exists
#      only to exercise the seam).
#   2. Removes the corresponding include() lines from settings.gradle.kts and
#      any dependency lines on stripped modules from client build scripts.
#   3. Copies scripts/client-only/overlay/ over the tree — full-file
#      replacements for sources whose seam-coupled parts have a meaningful
#      client-only counterpart (local fake auth, seam-less networking,
#      two-tab main).
#
# The result must build and pass tests; CI runs this script and verifies that
# on every PR (see the client-only-variant job in pr-verification.yml).

set -euo pipefail

cd "$(dirname "$0")/.."

STRIP_LIST="scripts/client-only/strip-paths.txt"
OVERLAY_DIR="scripts/client-only/overlay"

[[ -f "$STRIP_LIST" && -d "$OVERLAY_DIR" ]] || {
    echo "error: scripts/client-only/ tooling not found" >&2
    exit 1
}

echo "Stripping full-stack paths..."
while IFS= read -r path; do
    [[ -z "$path" || "$path" == \#* ]] && continue
    if [[ -e "$path" ]]; then
        rm -rf "$path"
        echo "  removed $path"
    else
        echo "error: $path listed in $STRIP_LIST does not exist (stale entry?)" >&2
        exit 1
    fi
done < "$STRIP_LIST"

echo "Pruning settings.gradle.kts includes..."
sed -i.bak \
    -e '/include(":server/d' \
    -e '/include(":shared/d' \
    -e '/include(":client:feature:notes/d' \
    -e '/aggregator(":server:app")/d' \
    -e '/^\/\/ =* :shared umbrella/d' \
    -e '/^\/\/ =* :server umbrella/d' \
    -e '/^\/\/ Server /d' \
    settings.gradle.kts
rm settings.gradle.kts.bak
cat -s settings.gradle.kts > settings.gradle.kts.squashed
mv settings.gradle.kts.squashed settings.gradle.kts

echo "Pruning dependencies on stripped modules..."
find client -name build.gradle.kts -not -path "*/build/*" -print0 |
    xargs -0 grep -l '":shared:\|":client:feature:notes:' 2>/dev/null |
    while IFS= read -r file; do
        sed -i.bak -e '/":shared:/d' -e '/":client:feature:notes:/d' "$file"
        rm "$file.bak"
        echo "  pruned $file"
    done

echo "Applying client-only overlay..."
(cd "$OVERLAY_DIR" && find . -type f -print0) | while IFS= read -r -d '' file; do
    file="${file#./}"
    mkdir -p "$(dirname "$file")"
    cp "$OVERLAY_DIR/$file" "$file"
    echo "  overlaid $file"
done

echo "Removing client-only tooling from the derived tree..."
rm -rf scripts/client-only scripts/make-client-only.sh

echo "Normalizing formatting (spotlessApply)..."
./gradlew --quiet spotlessApply

echo
echo "Done. This tree is now the client-only template."
echo "Verify with: ./gradlew jvmTest :client:androidApp:assembleDebug"
