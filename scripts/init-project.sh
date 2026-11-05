#!/usr/bin/env bash
#
# Turns this template into your project, in place. Run it once, on a fresh clone.
#
#   scripts/init-project.sh --name DemoApp --package com.example.demoapp --display-name "Demo App"
#
#   --name          PascalCase project name: root project, iOS product, Android class prefix.
#   --package       Kotlin package, Android application id, iOS and macOS bundle id.
#   --display-name  What users see: launcher label, iOS display name, desktop app and window title.
#   --no-example    Removes Tasks, main and settings: splash, login, then an empty home with logout.
#   --no-server     Also removes the server, shared contracts, integration tests, auth and the
#                   session: splash, then an empty home.
#
# The variants live under scripts/init/, each building on the last: delete.txt lists what goes,
# edits.patch changes what stays, and overlay/ holds files that are new or replaced whole. The script
# then renames everything, removes itself and its tooling, and formats the result. Nothing is
# committed, so the diff is yours to review.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

OLD_PACKAGE="org.example.project"
OLD_PACKAGE_PATH="org/example/project"
OLD_NAME="Template"
# Directories that hold no sources, and local files that hold no identity.
PRUNED_DIRS=(.git .gradle .kotlin .idea node_modules DerivedData)
KOTLIN_KEYWORDS=" as break class continue do else false for fun if in interface is null object
    package return super this throw true try typealias typeof val var when while "

die() {
    echo "error: $*" >&2
    exit 1
}

usage() {
    sed -n '3,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

need_value() {
    [[ $# -ge 2 && -n "$2" ]] || die "$1 needs a value"
}

NAME=""
PACKAGE=""
DISPLAY_NAME=""
VARIANT="default"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --name) need_value "$@"; NAME="$2"; shift 2 ;;
        --package) need_value "$@"; PACKAGE="$2"; shift 2 ;;
        --display-name) need_value "$@"; DISPLAY_NAME="$2"; shift 2 ;;
        --no-example | --no-server)
            [[ "$VARIANT" == default ]] || die "--no-example and --no-server exclude each other"
            VARIANT="${1#--}"
            shift
            ;;
        -h | --help) usage 0 ;;
        *) echo "error: unknown argument: $1" >&2; usage 2 ;;
    esac
done

[[ -n "$NAME" ]] || die "--name is required"
[[ -n "$PACKAGE" ]] || die "--package is required"
[[ -n "$DISPLAY_NAME" ]] || die "--display-name is required"

[[ "$NAME" =~ ^[A-Z][A-Za-z0-9]+$ ]] || die "--name must be PascalCase, such as DemoApp"
[[ "$NAME" != *"$OLD_NAME"* ]] || die "--name must not contain $OLD_NAME"

# No underscores: Apple bundle ids do not allow them.
[[ "$PACKAGE" =~ ^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$ ]] ||
    die "--package must be lowercase dot-separated letters and digits, such as com.example.demoapp"
[[ "$PACKAGE" != *"$OLD_PACKAGE"* ]] || die "--package must not contain $OLD_PACKAGE"
IFS='.' read -r -a package_segments <<<"$PACKAGE"
for segment in "${package_segments[@]}"; do
    [[ "$KOTLIN_KEYWORDS" != *[[:space:]]"$segment"[[:space:]]* ]] ||
        die "--package segment '$segment' is a Kotlin keyword"
done

# The display name lands in XML, Kotlin strings, an xcconfig and shell-quoted alert commands.
case "$DISPLAY_NAME" in
    *[\"\'\\\$\`/\<\>\|\&\;\*\?%]* | *$'\n'* | *$'\t'*)
        die "--display-name must not contain any of: \" ' \\ \$ \` / < > | & ; * ? %"
        ;;
esac

# The leftover check runs after the tree is rewritten, so anything it would flag is refused here.
[[ ! "$DISPLAY_NAME" =~ (^|[^[:alnum:]_])"$OLD_NAME"([^[:alnum:]_]|$) ]] ||
    die "--display-name must not contain the word $OLD_NAME"
for reserved in "$OLD_PACKAGE" TemplateApplication kmp-template init-project init-variants; do
    [[ "$DISPLAY_NAME" != *"$reserved"* ]] || die "--display-name must not contain $reserved"
done

if git rev-parse --is-inside-work-tree >/dev/null 2>&1 && [[ -n "$(git status --porcelain)" ]]; then
    die "the working tree has changes; run this on a fresh clone"
fi

# Prints every file the rename may touch, NUL-separated. A `build` directory under `src` is source.
project_files() {
    local prune=()
    for dir in "${PRUNED_DIRS[@]}"; do
        prune+=(-name "$dir" -o)
    done
    find . \( "${prune[@]}" -path './scripts/init' \) -prune -o \
        \( -type d -name build -not -path '*/src/*' \) -prune -o \
        -type f \
        -not -path ./scripts/init-project.sh \
        -not -path ./local.properties \
        -not -path ./.env \
        -not -path ./keystore.properties \
        -not -name '*.jks' \
        -not -name '*.keystore' \
        -print0
}

# Prints the text files containing the extended regex $1, one per line.
files_containing() {
    project_files | xargs -0 grep -IlE -e "$1" -- || true
}

# Applies the perl program $1 to every text file containing the regex $2.
rewrite_all() {
    local program="$1" pattern="$2" file
    while IFS= read -r file; do
        perl -pi -e "$program" "$file"
    done < <(files_containing "$pattern")
}

apply_variant() {
    local dir="scripts/init/$1" path
    echo "Applying $1"
    while IFS= read -r path; do
        [[ -z "$path" || "$path" == \#* ]] && continue
        [[ -e "$path" ]] || die "$dir/delete.txt lists '$path', which does not exist"
        rm -rf "$path"
    done <"$dir/delete.txt"
    git apply --whitespace=nowarn "$dir/edits.patch" ||
        die "$dir/edits.patch no longer applies; regenerate it against the current tree"
    cp -R "$dir/overlay/." .
}

strip() {
    case "$VARIANT" in
        no-example) apply_variant no-example ;;
        no-server)
            apply_variant no-example
            apply_variant no-server
            ;;
    esac
    # `rm` leaves the parents of what it removed.
    find client build-logic -type d -empty -delete
}

remove_init_ci_job() {
    local workflow=".github/workflows/pr-verification.yml"
    awk '
        /^  init-variants:$/ { skipping = 1; next }
        skipping && /^  [A-Za-z0-9_-]+:$/ { skipping = 0 }
        !skipping
    ' "$workflow" >"$workflow.tmp"
    mv "$workflow.tmp" "$workflow"
    perl -0pi -e 's/\n+\z/\n/' "$workflow"
}

# Moves $1 to $2, merging into whatever is there, and prunes the parents it leaves empty.
move_dir() {
    local src="$1" dst="$2" parent
    mkdir -p "$dst"
    cp -R "$src/." "$dst/"
    rm -rf "$src"
    parent="$(dirname "$src")"
    while [[ "$parent" != "." ]] && rmdir "$parent" 2>/dev/null; do
        parent="$(dirname "$parent")"
    done
}

rename_package() {
    local new_path dir base renamed
    new_path="$(echo "$PACKAGE" | tr '.' '/')"
    export OLD_PACKAGE OLD_PACKAGE_PATH PACKAGE new_path

    rewrite_all 's/\Q$ENV{OLD_PACKAGE}\E/$ENV{PACKAGE}/g; s/\Q$ENV{OLD_PACKAGE_PATH}\E/$ENV{new_path}/g' \
        "$OLD_PACKAGE|$OLD_PACKAGE_PATH"

    while IFS= read -r dir; do
        move_dir "$dir" "${dir%"$OLD_PACKAGE_PATH"}$new_path"
    done < <(find . -type d -path "*/$OLD_PACKAGE_PATH" -not -path './.git/*' -not -path '*/build/*' \
        -not -path './.gradle/*' -not -path './scripts/init/*')

    # Room's schema directory is named after the database class.
    while IFS= read -r dir; do
        base="$(basename "$dir")"
        renamed="${base//$OLD_PACKAGE/$PACKAGE}"
        mv "$dir" "$(dirname "$dir")/$renamed"
    done < <(find . -type d -name "*$OLD_PACKAGE*" -not -path './.git/*' -not -path '*/build/*' \
        -not -path './.gradle/*' -not -path './scripts/init/*')
}

rename_identity() {
    local slug android_app
    slug="$(echo "$NAME" | tr '[:upper:]' '[:lower:]')"
    export NAME DISPLAY_NAME slug

    perl -pi -e 's/rootProject\.name = "Template"/rootProject.name = "$ENV{NAME}"/' settings.gradle.kts

    # Android: strings are display, classes and themes are code.
    perl -pi -e 's/>Template( Dev)?</>$ENV{DISPLAY_NAME}$1</' \
        client/androidApp/src/main/res/values/strings.xml \
        client/androidApp/src/dev/res/values/strings.xml
    rewrite_all 's/TemplateApplication/$ENV{NAME}Application/g; s/Theme\.Template\b/Theme.$ENV{NAME}/g' \
        'TemplateApplication|Theme\.Template'
    android_app="$(find client/androidApp/src/main/java -name TemplateApplication.kt)"
    [[ -n "$android_app" ]] || die "TemplateApplication.kt not found"
    mv "$android_app" "$(dirname "$android_app")/${NAME}Application.kt"

    # Desktop: packageName and window title are both what the user sees.
    perl -pi -e 's/"Template( Dev)?"/"$ENV{DISPLAY_NAME}$1"/g' \
        client/desktopApp/build.gradle.kts \
        client/desktopApp/src/main/kotlin/"$(echo "$PACKAGE" | tr '.' '/')"/main.kt

    # iOS.
    perl -pi -e 's/^PRODUCT_NAME=Template$/PRODUCT_NAME=$ENV{NAME}/; s/^DISPLAY_NAME=Template$/DISPLAY_NAME=$ENV{DISPLAY_NAME}/' \
        iosApp/Configuration/Config.xcconfig
    perl -pi -e 's/\bTemplate\.app\b/$ENV{NAME}.app/g' \
        iosApp/iosApp.xcodeproj/project.pbxproj \
        iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/*.xcscheme \
        scripts/run-ios-device.sh

    # The server's default token issuer and audience. Gone with --no-server.
    rewrite_all 's/kmp-template/$ENV{slug}/g' 'kmp-template'
}

remove_tooling() {
    rm -rf scripts/init scripts/init-project.sh
}

fail_on_leftovers() {
    local hits
    hits="$(project_files | xargs -0 grep -InE \
        -e "$OLD_PACKAGE|$OLD_PACKAGE_PATH|TemplateApplication|kmp-template|Theme\.Template" \
        -e 'init-project|scripts/init|init-variants' -- || true)"
    hits+=$'\n'"$(project_files | xargs -0 grep -Inw -e "$OLD_NAME" -- || true)"
    [[ -z "${hits//$'\n'/}" ]] || die "template identity left behind:"$'\n'"$hits"
}

strip
remove_init_ci_job
echo "Renaming to $NAME ($PACKAGE)"
rename_package
rename_identity
remove_tooling
fail_on_leftovers

# Import order depends on the package, and a longer package can reflow lines.
echo "Formatting"
./gradlew --quiet spotlessApply || die "spotlessApply failed; fix the build and run it again"

cat <<DONE

$NAME is ready. Nothing is committed yet.
  - Set TEAM_ID in iosApp/Configuration/Config.xcconfig to sign iOS device builds.
  - Point the API base URL at your deployment (search for api.example.com).
  - Review, then commit: git add -A && git commit
DONE
