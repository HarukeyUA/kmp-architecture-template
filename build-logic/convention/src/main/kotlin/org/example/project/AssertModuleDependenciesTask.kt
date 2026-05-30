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
    // 1. Umbrella dependency law (ADR-0001): the umbrella name *is* the rule.
    //    :client -> :client | :shared ; :server -> :server | :shared ; :shared -> :shared only.
    //    :client <-> :server is forbidden.
    umbrellaViolation(sourcePath, targetPath)?.let {
        return it
    }

    // 2. Client layering: core is the foundation, features sit on top.
    if (sourcePath.startsWith(":client:core:") && targetPath.startsWith(":client:feature:")) {
        return "'$targetPath' not allowed — :core modules may not depend on :feature modules"
    }

    // 3. public/impl/testing rules. A `:shared:*` module is a flat contract (all-public by nature)
    //    and counts as a valid dependency target for both :public and :impl modules.
    val targetIsContract = targetPath.startsWith(":shared:")
    val targetType = moduleTypeOf(targetPath)
    return when (moduleTypeOf(sourcePath)) {
        ModuleType.UNKNOWN -> null
        ModuleType.PUBLIC ->
            if (targetType != ModuleType.PUBLIC && !targetIsContract) {
                "'$targetPath' not allowed — :public may only depend on :public or a :shared contract"
            } else null
        ModuleType.IMPL ->
            if (targetType != ModuleType.PUBLIC && !targetIsContract) {
                "'$targetPath' not allowed — :impl may only depend on :public or a :shared contract"
            } else null
        ModuleType.TESTING -> {
            val sibling = sourcePath.removeSuffix(":testing") + ":public"
            if (targetPath != sibling) {
                "'$targetPath' not allowed — :testing may only depend on its sibling '$sibling'"
            } else null
        }
    }
}

private fun umbrellaViolation(sourcePath: String, targetPath: String): String? {
    val source = umbrellaOf(sourcePath) ?: return null
    val target = umbrellaOf(targetPath) ?: return null
    val allowed =
        when (source) {
            "client" -> setOf("client", "shared")
            "server" -> setOf("server", "shared")
            "shared" -> setOf("shared")
            else -> emptySet()
        }
    return if (target !in allowed) {
        "'$targetPath' not allowed — :$source modules may depend only on " +
            allowed.sorted().joinToString(", ") { ":$it" }
    } else null
}

private fun umbrellaOf(path: String): String? =
    when {
        path.startsWith(":client:") -> "client"
        path.startsWith(":server:") -> "server"
        path.startsWith(":shared:") -> "shared"
        else -> null
    }
