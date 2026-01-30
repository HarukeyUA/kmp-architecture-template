package org.example.project

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.project.IsolatedProject
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

fun VersionCatalog.library(alias: String): String =
    findLibrary(alias).get().get().toString()

fun VersionCatalog.plugin(alias: String): String =
    findPlugin(alias).get().get().pluginId

fun KotlinMultiplatformExtension.androidLibrary(
    action: KotlinMultiplatformAndroidLibraryTarget.() -> Unit
) {
    extensions.getByType<KotlinMultiplatformAndroidLibraryTarget>().action()
}

/**
 * Determines the sibling :public module path from an :impl module.
 * e.g., :feature:auth:impl -> :feature:auth:public
 */
fun Project.siblingPublicModule(): String {
    val path = project.path
    require(path.endsWith(":impl")) {
        "siblingPublicModule() should only be called from :impl modules, but was called from $path"
    }
    return path.removeSuffix(":impl") + ":public"
}

fun Project.namespace(): String {
    val group = "org.example.project"

    val path =
        when {
            path.endsWith("public") -> requireParent().path
            else -> path
        }

    return "$group${path.replace(':', '.').replace('-', '.')}"
}

private fun Project.requireParent(): IsolatedProject =
    requireNotNull(parent).isolated

enum class ModuleType {
    PUBLIC,
    IMPL,
    UNKNOWN
}

val Project.moduleType: ModuleType
    get() {
        val name = path.substringAfterLast(':')
        return when {
            name == "public" -> ModuleType.PUBLIC
            name == "impl" -> ModuleType.IMPL
            else -> ModuleType.UNKNOWN
        }
    }

fun Project.isImplModule(): Boolean = moduleType == ModuleType.IMPL

fun Project.isPublicModule(): Boolean = moduleType == ModuleType.PUBLIC

private fun Project.localImplModules(rootProject: Project = this.rootProject): List<String> {
    return rootProject.subprojects
        .filter { it.subprojects.isEmpty() }
        .filter { it.isImplModule() }
        .filter { it.path != this.path }
        .map { it.path }
        .sorted()
}

fun KotlinDependencyHandler.addLocalImplDependencies(
    project: Project
) {
    project.localImplModules().forEach { implModulePath ->
        api(project.project(implModulePath))
    }
}
