import org.example.project.siblingPublicModule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base for a client `:testing` module that publishes reusable test support for its sibling
 * `:public` contract.
 */
class KmpFeatureTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("convention.kmp.library")
                apply("convention.coroutines")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets { commonMain.dependencies { api(project(siblingPublicModule())) } }
            }
        }
    }
}
