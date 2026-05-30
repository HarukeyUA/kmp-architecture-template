import org.example.project.configureServerKotlinJvm
import org.example.project.library
import org.example.project.libs
import org.example.project.siblingPublicModule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Base for a server domain `:impl` module (`:server:feature:<domain>:impl`): ServiceImpl,
 * Repository, Exposed tables, routes, stateful validation, and Metro bindings — organised as
 * `route/`, `service/`, `data/` packages. Bundles the per-domain staples: Metro, Arrow, Exposed,
 * and Ktor server (routes self-register as `RouteRegistrar`). The sibling `:public`, the
 * `:server:core:*` contracts, and the `:shared:<domain>` contract are wired per-module.
 */
class ServerFeatureImplConventionPlugin : Plugin<Project> {
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
                add("implementation", libs.library("ktor-server-core"))
                add("implementation", libs.library("exposed-core"))
                add("implementation", libs.library("exposed-jdbc"))
                add("testImplementation", libs.library("kotlin-test"))
                add("testImplementation", libs.library("assertk"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
