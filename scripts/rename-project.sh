#!/usr/bin/env bash
# Renames the KMP template project to a new name and package identifier.
#
# Usage:
#   ./scripts/rename-project.sh --name AwesomeApp --package com.domain.awesomeapp
#   ./scripts/rename-project.sh --name AwesomeApp --package com.domain.awesomeapp --display-name "Awesome App"
#   ./scripts/rename-project.sh --name AwesomeApp --package com.domain.awesomeapp --dry-run

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# ── Current template values ────────────────────────────────────────────────────
T_APP_NAME="MyApplication"          # PascalCase name used in class names, themes, etc.
T_DISPLAY_NAME="My Application"     # Human-readable display name in strings.xml
T_BASE_PKG="org.example.project"    # Shared Kotlin base package
T_ANDROID_PKG="com.rainy.myapplication"  # Android application ID / namespace
T_IOS_PRODUCT="KotlinProject"       # iOS product name in Config.xcconfig / pbxproj

# ── Helpers ────────────────────────────────────────────────────────────────────
usage() {
  cat <<EOF
Usage: $(basename "$0") --name <AppName> --package <com.company.app> [options]

Arguments:
  --name <PascalCase>       Application name in PascalCase, e.g. AwesomeApp
  --package <id>            Base package identifier, e.g. com.domain.awesomeapp
                            Used for: Android namespace/applicationId, iOS bundle ID,
                            and the shared Kotlin package.

Options:
  --display-name <string>   Human-readable app name shown in the launcher.
                            Defaults to PascalCase split: AwesomeApp → "Awesome App"
  --dry-run                 Preview all changes without modifying any files.
  -h, --help                Show this help.

Examples:
  $(basename "$0") --name WeatherApp --package com.example.weather
  $(basename "$0") --name MyApp --package io.myco.myapp --display-name "My App"
EOF
  exit "${1:-0}"
}

die()  { echo "ERROR: $*" >&2; exit 1; }
info() { echo "  $*"; }
step() { echo; echo "▶ $*"; }

# ── Parse arguments ────────────────────────────────────────────────────────────
NEW_APP_NAME=""
NEW_PACKAGE=""
NEW_DISPLAY_NAME=""
DRY_RUN=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --name)         NEW_APP_NAME="$2";     shift 2 ;;
    --package)      NEW_PACKAGE="$2";      shift 2 ;;
    --display-name) NEW_DISPLAY_NAME="$2"; shift 2 ;;
    --dry-run)      DRY_RUN=true;          shift   ;;
    -h|--help)      usage 0 ;;
    *) die "Unknown argument: $1  (run with --help for usage)" ;;
  esac
done

[[ -z "$NEW_APP_NAME" ]] && die "--name is required"
[[ -z "$NEW_PACKAGE"  ]] && die "--package is required"

# Validate: PascalCase, no spaces, starts with uppercase letter
[[ "$NEW_APP_NAME" =~ ^[A-Z][A-Za-z0-9]+$ ]] \
  || die "--name '$NEW_APP_NAME' must be PascalCase with no spaces (e.g. AwesomeApp)"

# Validate: lowercase dot-separated segments, at least two components
[[ "$NEW_PACKAGE" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] \
  || die "--package '$NEW_PACKAGE' must be lowercase dot-separated (e.g. com.acme.myapp)"

# Derive display name by splitting PascalCase: AwesomeApp → "Awesome App"
if [[ -z "$NEW_DISPLAY_NAME" ]]; then
  NEW_DISPLAY_NAME=$(echo "$NEW_APP_NAME" | sed 's/\([A-Z]\)/ \1/g' | sed 's/^ //')
fi

# Path form of the new package: com.acme.myapp → com/acme/myapp
NEW_PKG_PATH=$(echo "$NEW_PACKAGE" | tr '.' '/')
OLD_BASE_PATH="org/example/project"
OLD_ANDROID_PATH="com/rainy/myapplication"

# ── Summary ────────────────────────────────────────────────────────────────────
echo "┌─────────────────────────────────────────────────────────────┐"
echo "│                Rename KMP Template Project                  │"
echo "├─────────────────────────────────────────────────────────────┤"
printf "│  %-20s  %-22s → %s\n" "App name:"      "$T_APP_NAME"      "$NEW_APP_NAME"
printf "│  %-20s  %-22s → %s\n" "Display name:"  "$T_DISPLAY_NAME"  "$NEW_DISPLAY_NAME"
printf "│  %-20s  %-22s → %s\n" "Base package:"  "$T_BASE_PKG"      "$NEW_PACKAGE"
printf "│  %-20s  %-22s → %s\n" "Android ID:"    "$T_ANDROID_PKG"   "$NEW_PACKAGE"
printf "│  %-20s  %-22s → %s\n" "iOS product:"   "$T_IOS_PRODUCT"   "$NEW_APP_NAME"
printf "│  %-20s  %-22s → %s\n" "iOS bundle ID:" "$T_BASE_PKG.$T_IOS_PRODUCT" "$NEW_PACKAGE"
echo "└─────────────────────────────────────────────────────────────┘"
$DRY_RUN && echo "(dry-run mode — no files will be modified)"

# ── Pre-flight checks ──────────────────────────────────────────────────────────
if ! $DRY_RUN; then
  if ! git -C "$ROOT_DIR" diff --quiet 2>/dev/null || \
     ! git -C "$ROOT_DIR" diff --cached --quiet 2>/dev/null; then
    echo
    echo "WARNING: You have uncommitted changes. It is strongly recommended to"
    echo "         commit or stash them before renaming so you can review the diff."
    read -r -p "Continue anyway? [y/N] " answer
    [[ "${answer,,}" == "y" ]] || { echo "Aborted."; exit 0; }
  fi
fi

# ── Utilities ──────────────────────────────────────────────────────────────────

# Portable in-place sed (macOS requires '' argument, GNU sed does not)
_sed_inplace() {
  local file="$1"; shift
  if [[ "$(uname)" == "Darwin" ]]; then
    sed -i '' "$@" "$file"
  else
    sed -i "$@" "$file"
  fi
}

# Replace all occurrences of $2 with $3 in file $1
do_replace() {
  local file="$1" old="$2" new="$3"
  [[ -f "$file" ]] || return 0
  [[ "$old" == "$new" ]] && return 0
  if $DRY_RUN; then
    grep -qF "$old" "$file" 2>/dev/null \
      && info "[would edit] ${file#"$ROOT_DIR/"}: '$old' → '$new'"
    return 0
  fi
  _sed_inplace "$file" "s|${old}|${new}|g"
}

# Find all text source files, excluding generated / VCS directories
find_text_files() {
  find "$ROOT_DIR" \
    -not -path "*/.git/*"    \
    -not -path "*/.gradle/*" \
    -not -path "*/build/*"   \
    -not -path "*/.kotlin/*" \
    -not -path "*/.idea/*"   \
    -type f \( \
      -name "*.kt"       -o \
      -name "*.kts"      -o \
      -name "*.xml"      -o \
      -name "*.swift"    -o \
      -name "*.pbxproj"  -o \
      -name "*.xcconfig" -o \
      -name "*.plist"    -o \
      -name "*.properties" \
    \) -print0
}

# Replace $1 → $2 in every text source file
replace_in_sources() {
  local old="$1" new="$2"
  [[ "$old" == "$new" ]] && return 0
  while IFS= read -r -d '' file; do
    do_replace "$file" "$old" "$new"
  done < <(find_text_files)
}

# Move a directory tree from $1 to $2, pruning empty ancestor dirs of the old path
move_dir() {
  local src="$1" dst="$2"
  [[ "$src" == "$dst" ]] && return 0
  if $DRY_RUN; then
    [[ -d "$src" ]] && info "[would move] ${src#"$ROOT_DIR/"} → ${dst#"$ROOT_DIR/"}"
    return 0
  fi
  [[ -d "$src" ]] || return 0
  mkdir -p "$dst"
  cp -r "$src/." "$dst/"
  rm -rf "$src"
  # Prune empty ancestor directories left behind by the move
  local parent
  parent="$(dirname "$src")"
  while [[ "$parent" != "$ROOT_DIR" && -d "$parent" ]]; do
    rmdir "$parent" 2>/dev/null || break
    parent="$(dirname "$parent")"
  done
}

# ── Step 1: Replace package identifiers ───────────────────────────────────────
step "Replacing package identifiers"
info "org.example.project → $NEW_PACKAGE"
replace_in_sources "$T_BASE_PKG" "$NEW_PACKAGE"

info "com.rainy.myapplication → $NEW_PACKAGE"
replace_in_sources "$T_ANDROID_PKG" "$NEW_PACKAGE"

# ── Step 2: Replace iOS product name ──────────────────────────────────────────
step "Replacing iOS product name ($T_IOS_PRODUCT → $NEW_APP_NAME)"
replace_in_sources "$T_IOS_PRODUCT" "$NEW_APP_NAME"

# ── Step 3: Replace app name references ───────────────────────────────────────
# "MyApplication" appears in: class declarations, theme names, manifest, root project name
step "Replacing app name references (MyApplication → $NEW_APP_NAME)"
replace_in_sources "$T_APP_NAME" "$NEW_APP_NAME"

# ── Step 4: Replace display name in strings.xml ───────────────────────────────
step "Updating display name in strings.xml"
do_replace \
  "$ROOT_DIR/androidApp/src/main/res/values/strings.xml" \
  "$T_DISPLAY_NAME" "$NEW_DISPLAY_NAME"

# ── Step 5: Rename MyApplication.kt → <NewAppName>.kt ─────────────────────────
step "Renaming Application class file"
OLD_APP_KT="$ROOT_DIR/androidApp/src/main/java/$OLD_ANDROID_PATH/${T_APP_NAME}.kt"
NEW_APP_KT="$ROOT_DIR/androidApp/src/main/java/$OLD_ANDROID_PATH/${NEW_APP_NAME}.kt"
if [[ "$OLD_APP_KT" != "$NEW_APP_KT" ]]; then
  if $DRY_RUN; then
    [[ -f "$OLD_APP_KT" ]] && info "[would rename] ${T_APP_NAME}.kt → ${NEW_APP_NAME}.kt"
  else
    [[ -f "$OLD_APP_KT" ]] && mv "$OLD_APP_KT" "$NEW_APP_KT"
  fi
fi

# ── Step 6: Relocate Android source directories ────────────────────────────────
step "Relocating Android source directories ($OLD_ANDROID_PATH → $NEW_PKG_PATH)"
for src_set in main test androidTest; do
  move_dir \
    "$ROOT_DIR/androidApp/src/$src_set/java/$OLD_ANDROID_PATH" \
    "$ROOT_DIR/androidApp/src/$src_set/java/$NEW_PKG_PATH"
done

# ── Step 7: Relocate shared Kotlin source directories ─────────────────────────
step "Relocating shared Kotlin source directories ($OLD_BASE_PATH → $NEW_PKG_PATH)"

# build-logic convention plugin
move_dir \
  "$ROOT_DIR/build-logic/convention/src/main/kotlin/$OLD_BASE_PATH" \
  "$ROOT_DIR/build-logic/convention/src/main/kotlin/$NEW_PKG_PATH"

# All source-set kotlin roots across all modules (commonMain, jvmMain, iosMain, etc.)
while IFS= read -r -d '' kotlin_root; do
  move_dir "$kotlin_root/$OLD_BASE_PATH" "$kotlin_root/$NEW_PKG_PATH"
done < <(find "$ROOT_DIR" \
    -not -path "*/.git/*"     \
    -not -path "*/.gradle/*"  \
    -not -path "*/build/*"    \
    -not -path "*/.kotlin/*"  \
    -not -path "*/build-logic/*" \
    -type d -name "kotlin" -print0)

# ── Done ───────────────────────────────────────────────────────────────────────
echo
if $DRY_RUN; then
  echo "Dry-run complete. Re-run without --dry-run to apply changes."
else
  echo "✓ Project renamed to '$NEW_APP_NAME' with package '$NEW_PACKAGE'."
  echo
  echo "Recommended next steps:"
  echo "  1. ./gradlew spotlessApply          # auto-fix any formatting"
  echo "  2. ./gradlew build                  # verify the build"
  echo "  3. Open iosApp/Configuration/Config.xcconfig and set TEAM_ID if needed"
  echo "  4. git add -A && git commit -m 'chore: rename project to $NEW_APP_NAME'"
fi
