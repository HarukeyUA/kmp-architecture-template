package org.example.project

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Validates module structure at settings evaluation time, when the full project tree is visible
 * without crossing project boundaries.
 *
 * Rules enforced:
 * 1. Every leaf project's name must be one of [ALLOWED_LEAF_NAMES]. Prevents ad-hoc module names
 *    that sidestep the convention.
 * 2. Every `:impl` project must have a sibling `:public` project. Prevents orphan implementation
 *    modules that lack a contract.
 */
class ModuleStructureAssertSettingsPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        target.gradle.settingsEvaluated {
            val violations = mutableListOf<String>()
            val allowedList = ALLOWED_LEAF_NAMES.sorted().joinToString(", ")

            target.rootProject.leafDescendants().forEach { leaf ->
                if (leaf.name !in ALLOWED_LEAF_NAMES) {
                    violations +=
                        "Leaf module '${leaf.path}' has unrecognized name '${leaf.name}'. " +
                            "Allowed: $allowedList."
                }
                if (leaf.name == "impl") {
                    val siblingPath = "${leaf.parent!!.path}:public"
                    if (target.findProject(siblingPath) == null) {
                        violations +=
                            "Impl module '${leaf.path}' has no sibling :public at '$siblingPath'."
                    }
                }
            }

            if (violations.isNotEmpty()) {
                throw GradleException(
                    buildString {
                        appendLine("Module structure violations:")
                        violations.forEach { appendLine("  - $it") }
                        append("See ARCHITECTURE.md § Module Structure.")
                    }
                )
            }
        }
    }

    private companion object {
        val ALLOWED_LEAF_NAMES =
            setOf("public", "impl", "testing", "composeApp", "androidApp", "desktopApp")
    }
}
