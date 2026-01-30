import org.example.project.siblingPublicModule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.example.kmp.library")
                apply("org.example.coroutines")
                apply("org.example.serialization")
                apply("org.example.metro")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets { commonMain.dependencies { api(project(siblingPublicModule())) } }
            }
        }
    }
}
