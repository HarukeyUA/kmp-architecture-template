import org.example.project.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpComposeFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.example.kmp.feature.impl")
                apply("org.example.compose")
                apply("org.example.metro")
                apply("org.example.molecule")
                apply("org.example.decompose")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(project(":core:component:impl"))
                    implementation(project(":core:ui:public"))
                }
            }

            extensions.configure<KotlinMultiplatformExtension> {
                androidLibrary {
                    androidResources {
                        enable = true
                    }
                }
            }
        }
    }
}
