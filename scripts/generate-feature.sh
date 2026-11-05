#!/usr/bin/env bash
#
# Generates a :public, :impl, :testing and :robots module set under :client.
#
#   scripts/generate-feature.sh <module_path> <ModuleName> [feature|core]
#
#   scripts/generate-feature.sh invoices Invoices          # :client:feature:invoices:*
#   scripts/generate-feature.sh user/details UserDetails   # :client:feature:user:details:*
#   scripts/generate-feature.sh metrics Metrics core       # :client:core:metrics:*
#
# The scaffold is a working slice: a Molecule component with an event sink, a screen with a test
# tag, a repository with its fake, a component test and a robot. Replace what you do not need.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TEMPLATES_DIR="$SCRIPT_DIR/templates/feature"
SETTINGS_FILE="$PROJECT_ROOT/settings.gradle.kts"

die() {
    echo "error: $*" >&2
    exit 1
}

usage() {
    sed -n '3,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//' >&2
    exit 1
}

[ $# -ge 2 ] && [ $# -le 3 ] || usage

MODULE_PATH="$1"
FEATURE_NAME="$2"
SCOPE="${3:-feature}"

[[ "$SCOPE" == feature || "$SCOPE" == core ]] || die "scope must be 'feature' or 'core', got '$SCOPE'"
[[ "$MODULE_PATH" =~ ^[a-z][a-z0-9-]*(/[a-z][a-z0-9-]*)*$ ]] ||
    die "module path must be lowercase segments such as 'invoices' or 'user/details'"
[[ "$FEATURE_NAME" =~ ^[A-Z][A-Za-z0-9]*$ ]] || die "module name must be PascalCase, got '$FEATURE_NAME'"

# The base package comes from a file every tree has, so this keeps working once the package changes.
ANCHOR_FILE=$(find "$PROJECT_ROOT/client/core/component/public/src/commonMain/kotlin" \
    -type f -name AppComponentContext.kt | head -n1)
[ -n "$ANCHOR_FILE" ] || die "could not find AppComponentContext.kt to detect the base package"
ANCHOR_PACKAGE=$(awk '/^package /{print $2; exit}' "$ANCHOR_FILE")
BASE_PACKAGE="${ANCHOR_PACKAGE%.core.component}"
[ "$BASE_PACKAGE" != "$ANCHOR_PACKAGE" ] || die "unexpected anchor package '$ANCHOR_PACKAGE'"

FEATURE_NAME_LOWER=$(echo "$FEATURE_NAME" | tr '[:upper:]' '[:lower:]')
PACKAGE_PATH="$BASE_PACKAGE.$SCOPE.$(echo "$MODULE_PATH" | tr '/-' '..')"
PACKAGE_DIR=$(echo "$PACKAGE_PATH" | tr '.' '/')
MODULE_DIR="$PROJECT_ROOT/client/$SCOPE/$MODULE_PATH"
GRADLE_PATH=":client:$SCOPE:$(echo "$MODULE_PATH" | tr '/' ':')"
TESTING_MODULE="$GRADLE_PATH:testing"

[ ! -e "$MODULE_DIR" ] || die "module directory already exists: $MODULE_DIR"

render() {
    local template="$TEMPLATES_DIR/$1" output="$MODULE_DIR/$2"
    mkdir -p "$(dirname "$output")"
    sed -e "s|{{FEATURE_NAME}}|$FEATURE_NAME|g" \
        -e "s|{{FEATURE_NAME_LOWER}}|$FEATURE_NAME_LOWER|g" \
        -e "s|{{PACKAGE_PATH}}|$PACKAGE_PATH|g" \
        -e "s|{{BASE_PACKAGE}}|$BASE_PACKAGE|g" \
        -e "s|{{TESTING_MODULE}}|$TESTING_MODULE|g" \
        "$template" >"$output"
    echo "  created client/$SCOPE/$MODULE_PATH/$2"
}

MAIN="src/commonMain/kotlin"
TEST="src/commonTest/kotlin"

render public/build.gradle.kts.template public/build.gradle.kts
render public/$MAIN/Component.kt.template "public/$MAIN/$PACKAGE_DIR/${FEATURE_NAME}Component.kt"
render public/$MAIN/Repository.kt.template "public/$MAIN/$PACKAGE_DIR/${FEATURE_NAME}Repository.kt"
render public/$MAIN/Screen.kt.template "public/$MAIN/$PACKAGE_DIR/${FEATURE_NAME}Screen.kt"

render impl/build.gradle.kts.template impl/build.gradle.kts
render impl/$MAIN/DefaultComponent.kt.template "impl/$MAIN/$PACKAGE_DIR/Default${FEATURE_NAME}Component.kt"
render impl/$MAIN/DefaultRepository.kt.template "impl/$MAIN/$PACKAGE_DIR/Default${FEATURE_NAME}Repository.kt"
render impl/$MAIN/Screen.kt.template "impl/$MAIN/$PACKAGE_DIR/Default${FEATURE_NAME}Screen.kt"
render impl/$MAIN/ScreenTestTags.kt.template "impl/$MAIN/$PACKAGE_DIR/${FEATURE_NAME}ScreenTestTags.kt"
render impl/$TEST/DefaultComponentTest.kt.template "impl/$TEST/$PACKAGE_DIR/Default${FEATURE_NAME}ComponentTest.kt"

render testing/build.gradle.kts.template testing/build.gradle.kts
render testing/$MAIN/FakeRepository.kt.template "testing/$MAIN/$PACKAGE_DIR/Fake${FEATURE_NAME}Repository.kt"

render robots/build.gradle.kts.template robots/build.gradle.kts
render robots/$MAIN/Robot.kt.template "robots/$MAIN/$PACKAGE_DIR/robots/${FEATURE_NAME}Robot.kt"

[ -z "$(tail -c1 "$SETTINGS_FILE")" ] || echo >>"$SETTINGS_FILE"
for leaf in public impl testing robots; do
    printf '\ninclude("%s:%s")\n' "$GRADLE_PATH" "$leaf" >>"$SETTINGS_FILE"
done

# Import order depends on the base package, which the templates cannot know.
(cd "$PROJECT_ROOT" && ./gradlew --quiet \
    "$GRADLE_PATH:public:spotlessApply" "$GRADLE_PATH:impl:spotlessApply" \
    "$GRADLE_PATH:testing:spotlessApply" "$GRADLE_PATH:robots:spotlessApply") ||
    die "spotlessApply failed; run it again after fixing the build"

echo
echo "Generated $GRADLE_PATH:{public,impl,testing,robots}."
echo "Next: give a parent a child for ${FEATURE_NAME}Component.Factory and ${FEATURE_NAME}Screen."
