package org.example.project

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.project.IsolatedProject
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Repo-wide JDK release, from the `jdk` catalog entry, the one place the version is defined. */
fun Project.jdkVersion(): Int = libs.version("jdk").toInt()

/** [jdkVersion] as the Kotlin compiler's bytecode target. */
fun Project.jdkJvmTarget(): JvmTarget = JvmTarget.fromTarget(libs.version("jdk"))

/**
 * Shared base for plain-JVM server modules (`:server:*`). Pins the toolchain to the repo-wide JDK,
 * which is also the deploy target, and enables the same opt-ins as the client so Metro annotations
 * and context parameters behave identically across the monorepo.
 */
fun Project.configureServerKotlinJvm() {
    // Leaf modules share a directory name (`impl`/`public`), so by default every server jar would
    // be `impl.jar`/`public.jar` and the :server:app distribution would collide collecting them
    // into lib/. Derive a unique archive name from the full module path instead.
    extensions.configure<BasePluginExtension> {
        archivesName.set(path.removePrefix(":").replace(':', '-'))
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(jdkVersion())
        compilerOptions {
            jvmTarget.set(jdkJvmTarget())
            freeCompilerArgs.addAll(
                "-Xannotation-default-target=param-property",
                "-Xcontext-parameters",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            )
        }
    }
}

/** Lets the repo-wide `./gradlew jvmTest` reach a plain-JVM server module's `test` task. */
fun Project.registerServerJvmTestAlias() {
    tasks.register("jvmTest") {
        description = "Alias for :test so the repo-wide jvmTest convention hits this module."
        dependsOn("test")
    }
}

fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

fun VersionCatalog.library(alias: String): String = findLibrary(alias).get().get().toString()

fun VersionCatalog.plugin(alias: String): String = findPlugin(alias).get().get().pluginId

fun KotlinMultiplatformExtension.androidLibrary(
    action: KotlinMultiplatformAndroidLibraryTarget.() -> Unit,
) {
    extensions.getByType<KotlinMultiplatformAndroidLibraryTarget>().action()
}

/**
 * Determines the sibling `:public` module path from an `:impl` or `:testing` module. For example,
 * `:client:feature:auth:impl` and `:client:feature:auth:testing` both resolve to
 * `:client:feature:auth:public`.
 */
fun Project.siblingPublicModule(): String {
    val path = project.path
    val sourceType = moduleTypeOf(path)
    require(sourceType == ModuleType.IMPL || sourceType == ModuleType.TESTING) {
        "siblingPublicModule() should only be called from :impl or :testing modules, but was " +
            "called from $path"
    }
    return path.substringBeforeLast(':') + ":public"
}

/**
 * Determines the sibling :impl module path from a :robots module. e.g., :client:feature:auth:robots
 * -> :client:feature:auth:impl
 */
fun Project.siblingImplModule(): String {
    val path = project.path
    require(path.endsWith(":robots")) {
        "siblingImplModule() should only be called from :robots modules, but was called from $path"
    }
    return path.removeSuffix(":robots") + ":impl"
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

private fun Project.requireParent(): IsolatedProject = requireNotNull(parent).isolated

enum class ModuleType {
    PUBLIC,
    IMPL,
    TESTING,
    ROBOTS,
    UNKNOWN,
}

fun moduleTypeOf(path: String): ModuleType =
    when (path.substringAfterLast(':')) {
        "public" -> ModuleType.PUBLIC
        "impl" -> ModuleType.IMPL
        "testing" -> ModuleType.TESTING
        "robots" -> ModuleType.ROBOTS
        else -> ModuleType.UNKNOWN
    }

val Project.moduleType: ModuleType
    get() = moduleTypeOf(path)

fun Project.isImplModule(): Boolean = moduleType == ModuleType.IMPL

fun Project.isPublicModule(): Boolean = moduleType == ModuleType.PUBLIC
