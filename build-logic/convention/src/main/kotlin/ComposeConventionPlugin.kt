import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("compose-runtime"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-material3"))
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-ui-tooling-preview"))
                    implementation(libs.library("androidx-lifecycle-runtimeCompose"))
                }
            }

            dependencies.add("androidRuntimeClasspath", libs.library("compose-ui-tooling"))
        }
    }
}
