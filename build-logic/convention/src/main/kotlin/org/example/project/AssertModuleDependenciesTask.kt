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
 * ARCHITECTURE.md / ARCHITECTURE_SERVER.md. The task reads only its own project's state, so it is
 * compatible with Gradle's project isolation.
 *
 * Rules enforced (derived from module path):
 * - The umbrella law: `:client` -> `:client`/`:shared`, `:server` -> `:server`/`:shared`, `:shared`
 *   -> `:shared` only; `:client` <-> `:server` is forbidden (ADR-0001).
 * - `:public` may depend only on `:public` or a `:shared` contract.
 * - `:impl` may depend only on `:public` or a `:shared` contract.
 * - `:testing` may depend only on its sibling `:public`.
 * - `:robots` may depend only on its sibling `:impl` (test tags) or `:client:core:robots` (the
 *   `Robot`/`Wait` base).
 * - No `:client:core:*` module may depend on a `:client:feature:*` module (layering rule).
 * - A `:shared:*` module's **external** dependency surface is rationed to [ALLOWED_SHARED_EXTERNAL]
 *   so the Seam can't rot into a god module.
 * - Anything else (`:client:composeApp`, `:server:app`, ...) is unrestricted within the umbrella
 *   law.
 */
@CacheableTask
abstract class AssertModuleDependenciesTask : DefaultTask() {
    @get:Input abstract val sourcePath: Property<String>

    @get:Input abstract val dependencyPaths: SetProperty<String>

    /** Declared external dependency coordinates (`group:name`); only populated for `:shared:*`. */
    @get:Input abstract val externalDependencyCoordinates: SetProperty<String>

    @get:OutputFile abstract val stampFile: RegularFileProperty

    @TaskAction
    fun check() {
        val source = sourcePath.get()
        val violations =
            dependencyPaths.get().mapNotNull { target -> violation(source, target) } +
                externalDependencyCoordinates.get().mapNotNull { coord ->
                    sharedSurfaceViolation(source, coord)
                }
        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Module dependency rule violations in $source:")
                    violations.sorted().forEach { appendLine("  - $it") }
                    append(
                        "See ARCHITECTURE.md / ARCHITECTURE_SERVER.md § Module Dependency Rules."
                    )
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
    umbrellaViolation(sourcePath, targetPath)?.let {
        return it
    }

    if (sourcePath.startsWith(":client:core:") && targetPath.startsWith(":client:feature:")) {
        return "'$targetPath' not allowed — :core modules may not depend on :feature modules"
    }

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
        // Robots drive one screen through the test tags declared next to it in the sibling :impl,
        // and extend the shared Robot/Wait base. Nothing else — cross-feature journeys compose in
        // the E2E flow layer (:client:androidApp androidTest), not in robots.
        ModuleType.ROBOTS -> {
            val sibling = sourcePath.removeSuffix(":robots") + ":impl"
            if (targetPath != sibling && targetPath != CORE_ROBOTS_PATH) {
                "'$targetPath' not allowed — :robots may only depend on its sibling '$sibling' " +
                    "or '$CORE_ROBOTS_PATH'"
            } else null
        }
    }
}

private const val CORE_ROBOTS_PATH = ":client:core:robots"

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

private fun sharedSurfaceViolation(sourcePath: String, coordinate: String): String? {
    if (!sourcePath.startsWith(":shared:")) return null
    if (isAllowedSharedExternal(coordinate)) return null
    return "external dependency '$coordinate' not allowed — :shared is rationed to " +
        "kotlinx.serialization, ktor-resources, arrow-core, kotlinx.datetime"
}

private fun isAllowedSharedExternal(coordinate: String): Boolean {
    val group = coordinate.substringBefore(':')
    val name = coordinate.substringAfter(':', missingDelimiterValue = "")
    return when {
        group == "org.jetbrains.kotlin" -> true // Kotlin stdlib / kotlin-test
        group == "org.jetbrains.kotlinx" &&
            (name.startsWith("kotlinx-serialization") || name.startsWith("kotlinx-datetime")) ->
            true
        coordinate == "io.ktor:ktor-resources" -> true
        coordinate == "io.arrow-kt:arrow-core" -> true
        else -> false
    }
}
