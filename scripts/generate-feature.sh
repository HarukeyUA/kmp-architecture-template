#!/bin/bash

# Module Generator
# Generates public and impl modules following the app's architectural patterns.
#
# Usage: ./scripts/generate-feature.sh <module_path> <module_name> [scope]
# Example: ./scripts/generate-feature.sh settings Settings
#          ./scripts/generate-feature.sh settings Settings feature
#          ./scripts/generate-feature.sh datastore Datastore core

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TEMPLATES_DIR="$SCRIPT_DIR/templates/feature"

# Validate arguments
if [ $# -lt 2 ]; then
    echo -e "${RED}Error: Missing arguments${NC}"
    echo ""
    echo "Usage: $0 <module_path> <module_name> [scope]"
    echo ""
    echo "Arguments:"
    echo "  module_path  - Path for the module (e.g., 'settings' or 'user/details')"
    echo "  module_name  - PascalCase name for the module (e.g., 'Settings' or 'UserDetails')"
    echo "  scope        - Optional scope: 'feature' (default) or 'core'"
    echo ""
    echo "Examples:"
    echo "  $0 settings Settings            # creates client:feature:settings:public/impl"
    echo "  $0 user/details UserDetails     # creates client:feature:user:details:public/impl"
    echo "  $0 local-storage LocalStorage core  # creates client:core:local-storage:public/impl"
    echo ""
    echo "Note: 'core' scope reuses the feature template (Component + Screen). For"
    echo "      infrastructure-only core modules (no UI), edit the generated files"
    echo "      to remove the Compose/Component scaffolding."
    exit 1
fi

MODULE_PATH="$1"
MODULE_NAME="$2"
SCOPE="${3:-feature}"

# Validate scope
if [ "$SCOPE" != "feature" ] && [ "$SCOPE" != "core" ]; then
    echo -e "${RED}Error: Invalid scope '$SCOPE'. Must be 'feature' or 'core'${NC}"
    exit 1
fi

# Detect the project's base package by reading the package declaration of a known
# anchor file (`AppComponentContext.kt` in `:client:core:component:public`) and stripping
# the `.core.component` suffix. This keeps the script working after `rename-project.sh`
# changes the base package away from `org.example.project`.
ANCHOR_FILE=$(find "$PROJECT_ROOT/client/core/component/public/src/commonMain/kotlin" \
    -type f -name "AppComponentContext.kt" 2>/dev/null | head -n1)
if [ -z "$ANCHOR_FILE" ] || [ ! -f "$ANCHOR_FILE" ]; then
    echo -e "${RED}Error: Could not locate AppComponentContext.kt to detect the base package.${NC}"
    echo "       Expected under: client/core/component/public/src/commonMain/kotlin/"
    exit 1
fi
ANCHOR_PACKAGE=$(awk '/^package /{print $2; exit}' "$ANCHOR_FILE")
BASE_PACKAGE="${ANCHOR_PACKAGE%.core.component}"
if [ -z "$BASE_PACKAGE" ] || [ "$BASE_PACKAGE" = "$ANCHOR_PACKAGE" ]; then
    echo -e "${RED}Error: Failed to derive base package from '$ANCHOR_PACKAGE'.${NC}"
    echo "       Expected a package ending in '.core.component'."
    exit 1
fi

# Template variable aliases
FEATURE_PATH="$MODULE_PATH"
FEATURE_NAME="$MODULE_NAME"
FEATURE_NAME_LOWER=$(echo "$MODULE_NAME" | tr '[:upper:]' '[:lower:]')

# Convert module path to package format:
# - Replace / with . (for nested paths like user/details)
# - Replace - with . (for hyphenated names like local-storage -> local.storage)
MODULE_PACKAGE_PATH=$(echo "$MODULE_PATH" | tr '/-' '..')

# Full package path
PACKAGE_PATH="$BASE_PACKAGE.$SCOPE.$MODULE_PACKAGE_PATH"

# Module paths (client modules live under the :client umbrella — see ARCHITECTURE.md)
MODULE_DIR="$PROJECT_ROOT/client/$SCOPE/$MODULE_PATH"
PUBLIC_DIR="$MODULE_DIR/public"
IMPL_DIR="$MODULE_DIR/impl"

# Gradle module paths (using : separator)
GRADLE_MODULE_PATH=$(echo "$MODULE_PATH" | tr '/' ':')
PUBLIC_MODULE=":client:$SCOPE:$GRADLE_MODULE_PATH:public"
IMPL_MODULE=":client:$SCOPE:$GRADLE_MODULE_PATH:impl"

echo -e "${GREEN}Generating $SCOPE module: $MODULE_NAME${NC}"
echo "  Path: $MODULE_PATH"
echo "  Scope: $SCOPE"
echo "  Base package: $BASE_PACKAGE"
echo "  Package: $PACKAGE_PATH"
echo "  Modules: $PUBLIC_MODULE, $IMPL_MODULE"
echo ""

# Check if module already exists
if [ -d "$MODULE_DIR" ]; then
    echo -e "${RED}Error: Module directory already exists: $MODULE_DIR${NC}"
    exit 1
fi

# Check if templates exist
if [ ! -d "$TEMPLATES_DIR" ]; then
    echo -e "${RED}Error: Templates directory not found: $TEMPLATES_DIR${NC}"
    exit 1
fi

# Function to process template file
process_template() {
    local template_file="$1"
    local output_file="$2"

    mkdir -p "$(dirname "$output_file")"

    sed -e "s|{{FEATURE_NAME}}|$FEATURE_NAME|g" \
        -e "s|{{FEATURE_NAME_LOWER}}|$FEATURE_NAME_LOWER|g" \
        -e "s|{{FEATURE_PATH}}|$FEATURE_PATH|g" \
        -e "s|{{PACKAGE_PATH}}|$PACKAGE_PATH|g" \
        -e "s|{{BASE_PACKAGE}}|$BASE_PACKAGE|g" \
        "$template_file" > "$output_file"

    echo "  Created: $output_file"
}

# Common kotlin source root directory (relative to a module dir)
PACKAGE_DIR_REL="src/commonMain/kotlin/$(echo "$PACKAGE_PATH" | tr '.' '/')"
TEST_PACKAGE_DIR_REL="src/commonTest/kotlin/$(echo "$PACKAGE_PATH" | tr '.' '/')"

# Create directory structure
echo "Creating directory structure..."
mkdir -p "$PUBLIC_DIR/$PACKAGE_DIR_REL"
mkdir -p "$IMPL_DIR/$PACKAGE_DIR_REL"
mkdir -p "$IMPL_DIR/$TEST_PACKAGE_DIR_REL"

# Process public module templates
echo ""
echo "Creating public module..."
process_template "$TEMPLATES_DIR/public/build.gradle.kts.template" "$PUBLIC_DIR/build.gradle.kts"
process_template "$TEMPLATES_DIR/public/src/commonMain/kotlin/Component.kt.template" \
    "$PUBLIC_DIR/$PACKAGE_DIR_REL/${FEATURE_NAME}Component.kt"
process_template "$TEMPLATES_DIR/public/src/commonMain/kotlin/Screen.kt.template" \
    "$PUBLIC_DIR/$PACKAGE_DIR_REL/${FEATURE_NAME}Screen.kt"

# Process impl module templates
echo ""
echo "Creating impl module..."
process_template "$TEMPLATES_DIR/impl/build.gradle.kts.template" "$IMPL_DIR/build.gradle.kts"
process_template "$TEMPLATES_DIR/impl/src/commonMain/kotlin/DefaultComponent.kt.template" \
    "$IMPL_DIR/$PACKAGE_DIR_REL/Default${FEATURE_NAME}Component.kt"
process_template "$TEMPLATES_DIR/impl/src/commonMain/kotlin/Screen.kt.template" \
    "$IMPL_DIR/$PACKAGE_DIR_REL/Default${FEATURE_NAME}Screen.kt"
process_template "$TEMPLATES_DIR/impl/src/commonTest/kotlin/DefaultComponentTest.kt.template" \
    "$IMPL_DIR/$TEST_PACKAGE_DIR_REL/Default${FEATURE_NAME}ComponentTest.kt"

# Update settings.gradle.kts
SETTINGS_FILE="$PROJECT_ROOT/settings.gradle.kts"
echo ""
echo "Updating settings.gradle.kts..."

if grep -q "include(\"$PUBLIC_MODULE\")" "$SETTINGS_FILE"; then
    echo -e "${YELLOW}Warning: $PUBLIC_MODULE already exists in settings.gradle.kts${NC}"
else
    # Append to the end of the file. The Gradle Kotlin formatter will normalize
    # blank-line spacing on the next `spotlessApply` / IDE format.
    [ -n "$(tail -c1 "$SETTINGS_FILE")" ] && echo "" >> "$SETTINGS_FILE"
    echo "" >> "$SETTINGS_FILE"
    echo "include(\"$PUBLIC_MODULE\")" >> "$SETTINGS_FILE"
    echo "" >> "$SETTINGS_FILE"
    echo "include(\"$IMPL_MODULE\")" >> "$SETTINGS_FILE"

    echo "  Added: $PUBLIC_MODULE"
    echo "  Added: $IMPL_MODULE"
fi

echo ""
echo -e "${GREEN}Module generated successfully!${NC}"

# Run Spotless on the new modules so import ordering matches the project's
# detected base package (templates can't pre-sort imports against an unknown package).
echo ""
echo "Running ./gradlew spotlessApply on the new modules..."
if [ -x "$PROJECT_ROOT/gradlew" ]; then
    if (cd "$PROJECT_ROOT" && ./gradlew --quiet \
        "$PUBLIC_MODULE:spotlessApply" \
        "$IMPL_MODULE:spotlessApply"); then
        echo "  Spotless reformatting complete."
    else
        echo -e "${YELLOW}  Spotless run failed — re-run './gradlew spotlessApply' manually.${NC}"
    fi
else
    echo -e "${YELLOW}  gradlew not found — run './gradlew spotlessApply' manually.${NC}"
fi

echo ""
echo "Next step: wire the new component into its parent (factory + child stack/tab)."
