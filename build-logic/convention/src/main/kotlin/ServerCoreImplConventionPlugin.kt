import org.example.project.configureServerKotlinJvm
import org.example.project.library
import org.example.project.libs
import org.example.project.siblingPublicModule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Base for a server-core `:impl` module (`:server:core:*:impl`). Adds Metro DI on top of the JVM
 * baseline and wires the sibling `:public` as an `api` dependency, mirroring the client's
 * `kmp.feature.impl`. Test deps cover the kotlin-test + AssertK + coroutines-test stack used across
 * the server (Testcontainers is added per-module, since only DB-touching modules need it).
 */
class ServerCoreImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("convention.spotless")
                apply("convention.detekt")
                apply("convention.module-graph-assert")
                apply("convention.metro")
            }

            configureServerKotlinJvm()

            dependencies {
                add("api", project(siblingPublicModule()))
                add("implementation", libs.library("kotlinx-coroutines-core"))
                add("implementation", libs.library("arrow-core"))
                add("testImplementation", libs.library("kotlin-test"))
                add("testImplementation", libs.library("assertk"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
