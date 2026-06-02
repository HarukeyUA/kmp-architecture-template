import org.example.project.configureServerKotlinJvm
import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Base for a server-core `:public` module (`:server:core:*:public`). Module-specific dependencies
 * (Ktor, Exposed, `:shared:*`) are declared per-module; only the baseline every server contract
 * uses (Arrow `Either`, coroutines) is provided here.
 */
class ServerCorePublicConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("convention.spotless")
                apply("convention.detekt")
                apply("convention.module-graph-assert")
            }

            configureServerKotlinJvm()

            dependencies {
                add("implementation", libs.library("kotlinx-coroutines-core"))
                add("api", libs.library("arrow-core"))
            }
        }
    }
}
