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
    echo "  $0 settings Settings            # creates feature:settings:public/impl"
    echo "  $0 user/details UserDetails     # creates feature:user:details:public/impl"
    echo "  $0 local-storage LocalStorage core  # creates core:local-storage:public/impl"
    echo "                                      # package: org.example.project.core.local.storage"
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

# Template variable aliases
FEATURE_PATH="$MODULE_PATH"
FEATURE_NAME="$MODULE_NAME"
FEATURE_NAME_LOWER=$(echo "$MODULE_NAME" | tr '[:upper:]' '[:lower:]')

# Convert module path to package format:
# - Replace / with . (for nested paths like user/details)
# - Replace - with . (for hyphenated names like local-storage -> local.storage)
MODULE_PACKAGE_PATH=$(echo "$MODULE_PATH" | tr '/-' '..')

# Full package path
PACKAGE_PATH="org.example.project.$SCOPE.$MODULE_PACKAGE_PATH"

# Module paths
MODULE_DIR="$PROJECT_ROOT/$SCOPE/$MODULE_PATH"
PUBLIC_DIR="$MODULE_DIR/public"
IMPL_DIR="$MODULE_DIR/impl"

# Gradle module paths (using : separator)
GRADLE_MODULE_PATH=$(echo "$MODULE_PATH" | tr '/' ':')
PUBLIC_MODULE=":$SCOPE:$GRADLE_MODULE_PATH:public"
IMPL_MODULE=":$SCOPE:$GRADLE_MODULE_PATH:impl"

echo -e "${GREEN}Generating $SCOPE module: $MODULE_NAME${NC}"
echo "  Path: $MODULE_PATH"
echo "  Scope: $SCOPE"
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

    # Create output directory if it doesn't exist
    mkdir -p "$(dirname "$output_file")"

    # Process template: replace placeholders
    sed -e "s|{{FEATURE_NAME}}|$FEATURE_NAME|g" \
        -e "s|{{FEATURE_NAME_LOWER}}|$FEATURE_NAME_LOWER|g" \
        -e "s|{{FEATURE_PATH}}|$FEATURE_PATH|g" \
        -e "s|{{PACKAGE_PATH}}|$PACKAGE_PATH|g" \
        "$template_file" > "$output_file"

    echo "  Created: $output_file"
}

# Create directory structure
echo "Creating directory structure..."
mkdir -p "$PUBLIC_DIR/src/commonMain/kotlin/$(echo "$PACKAGE_PATH" | tr '.' '/')"
mkdir -p "$IMPL_DIR/src/commonMain/kotlin/$(echo "$PACKAGE_PATH" | tr '.' '/')"

# Process public module templates
echo ""
echo "Creating public module..."
process_template "$TEMPLATES_DIR/public/build.gradle.kts.template" "$PUBLIC_DIR/build.gradle.kts"
process_template "$TEMPLATES_DIR/public/src/commonMain/kotlin/Component.kt.template" \
    "$PUBLIC_DIR/src/commonMain/kotlin/$(echo "$PACKAGE_PATH" | tr '.' '/')/${FEATURE_NAME}Component.kt"
process_template "$TEMPLATES_DIR/public/src/commonMain/kotlin/Screen.kt.template" \
    "$PUBLIC_DIR/src/commonMain/kotlin/$(echo "$PACKAGE_PATH" | tr '.' '/')/${FEATURE_NAME}Screen.kt"

# Process impl module templates
echo ""
echo "Creating impl module..."
process_template "$TEMPLATES_DIR/impl/build.gradle.kts.template" "$IMPL_DIR/build.gradle.kts"
process_template "$TEMPLATES_DIR/impl/src/commonMain/kotlin/DefaultComponent.kt.template" \
    "$IMPL_DIR/src/commonMain/kotlin/$(echo "$PACKAGE_PATH" | tr '.' '/')/Default${FEATURE_NAME}Component.kt"
process_template "$TEMPLATES_DIR/impl/src/commonMain/kotlin/Screen.kt.template" \
    "$IMPL_DIR/src/commonMain/kotlin/$(echo "$PACKAGE_PATH" | tr '.' '/')/Default${FEATURE_NAME}Screen.kt"

# Update settings.gradle.kts
SETTINGS_FILE="$PROJECT_ROOT/settings.gradle.kts"
echo ""
echo "Updating settings.gradle.kts..."

# Check if modules are already in settings
if grep -q "include(\"$PUBLIC_MODULE\")" "$SETTINGS_FILE"; then
    echo -e "${YELLOW}Warning: $PUBLIC_MODULE already exists in settings.gradle.kts${NC}"
else
    # Find the last feature module line and add after it
    # This uses awk to find the last line starting with 'include(":feature:' and insert after it
    awk -v pub="include(\"$PUBLIC_MODULE\")" -v impl="include(\"$IMPL_MODULE\")" '
    {
        print
        if (/^include\(":feature:/ && !added) {
            last_feature_line = NR
            last_line = $0
        }
    }
    END {
        if (!found_end) {
            # If we processed all lines, append at the end
        }
    }
    ' "$SETTINGS_FILE" > "$SETTINGS_FILE.tmp"

    # Alternative approach: append to the end of the file
    # First check if file ends with newline
    if [ -n "$(tail -c1 "$SETTINGS_FILE")" ]; then
        echo "" >> "$SETTINGS_FILE"
    fi

    echo "include(\"$PUBLIC_MODULE\")" >> "$SETTINGS_FILE"
    echo "include(\"$IMPL_MODULE\")" >> "$SETTINGS_FILE"

    rm -f "$SETTINGS_FILE.tmp"

    echo "  Added: $PUBLIC_MODULE"
    echo "  Added: $IMPL_MODULE"
fi

echo ""
echo -e "${GREEN}Module generated successfully!${NC}"
