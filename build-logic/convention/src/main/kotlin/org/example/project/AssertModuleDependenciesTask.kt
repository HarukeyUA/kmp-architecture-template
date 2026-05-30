package org.example.project

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Validates that every declared project dependency on this module obeys the rules in
 * ARCHITECTURE.md. The task reads only its own project's state, so it is compatible with Gradle's
 * project isolation.
 *
 * Rules enforced (derived from module path):
 * - `:public` may depend only on `:public`.
 * - `:impl` may depend only on `:public`.
 * - `:testing` may depend only on its sibling `:public` (e.g. `:client:feature:x:testing` ->
 *   `:client:feature:x:public`).
 * - Anything else (`:client:composeApp`, `:client:androidApp`, ...) is unrestricted.
 * - In addition, no `:client:core:*` module may depend on a `:client:feature:*` module (layering
 *   rule).
 */
@CacheableTask
abstract class AssertModuleDependenciesTask : DefaultTask() {
    @get:Input abstract val sourcePath: Property<String>

    @get:Input abstract val dependencyPaths: SetProperty<String>

    @get:OutputFile abstract val stampFile: RegularFileProperty

    @TaskAction
    fun check() {
        val source = sourcePath.get()
        val violations = dependencyPaths.get().mapNotNull { target -> violation(source, target) }
        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Module dependency rule violations in $source:")
                    violations.sorted().forEach { appendLine("  - $it") }
                    append("See ARCHITECTURE.md § Module Dependency Rules.")
                }
            )
        }
        stampFile.get().asFile.apply {
            parentFile?.mkdirs()
            writeText("ok")
        }
    }
}

private fun violation(sourcePath: String, targetPath: String): String? {
    if (sourcePath.startsWith(":client:core:") && targetPath.startsWith(":client:feature:")) {
        return "'$targetPath' not allowed — :core modules may not depend on :feature modules"
    }
    val targetType = moduleTypeOf(targetPath)
    return when (moduleTypeOf(sourcePath)) {
        ModuleType.UNKNOWN -> null
        ModuleType.PUBLIC ->
            if (targetType != ModuleType.PUBLIC) {
                "'$targetPath' not allowed — :public may only depend on :public"
            } else null
        ModuleType.IMPL ->
            if (targetType != ModuleType.PUBLIC) {
                "'$targetPath' not allowed — :impl may only depend on :public"
            } else null
        ModuleType.TESTING -> {
            val sibling = sourcePath.removeSuffix(":testing") + ":public"
            if (targetPath != sibling) {
                "'$targetPath' not allowed — :testing may only depend on its sibling '$sibling'"
            } else null
        }
    }
}
