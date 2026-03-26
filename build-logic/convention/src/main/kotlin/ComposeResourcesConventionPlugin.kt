import org.example.project.androidLibrary
import org.example.project.library
import org.example.project.libs
import org.example.project.namespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeResourcesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("compose-resources"))
                }

                androidLibrary { androidResources { enable = true } }
            }

            extensions.configure<ComposeExtension> {
                extensions.configure<ResourcesExtension> { packageOfResClass = namespace() }
            }
        }
    }
}
