import org.example.project.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpComposeFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("convention.kmp.feature.impl")
                apply("convention.compose")
                apply("convention.metro")
                apply("convention.molecule")
                apply("convention.decompose")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(project(":core:component:public"))
                    implementation(project(":core:ui:public"))
                }
            }

            extensions.configure<KotlinMultiplatformExtension> {
                androidLibrary { androidResources { enable = true } }
            }
        }
    }
}
