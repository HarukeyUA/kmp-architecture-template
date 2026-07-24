import org.example.project.configureServerKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A server `:testing` module: publishes reusable fakes of its sibling `:public` contract, staying
 * plain JVM while sharing the server toolchain and compiler settings. The sibling contract is
 * exposed automatically, matching the client `:testing` convention, and the module-graph assert
 * keeps the dependency surface pinned to that sibling.
 */
class ServerTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("convention.spotless")
                apply("convention.detekt")
                apply("convention.module-graph-assert")
            }

            configureServerKotlinJvm()

            dependencies { add("api", project(path.removeSuffix(":testing") + ":public")) }
        }
    }
}
