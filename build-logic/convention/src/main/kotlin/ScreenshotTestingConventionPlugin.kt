import org.example.project.androidLibrary
import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ScreenshotTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) { apply("io.github.takahirom.roborazzi") }

            extensions.configure<KotlinMultiplatformExtension> {
                androidLibrary {
                    withHostTest {
                        isIncludeAndroidResources = true
                        isReturnDefaultValues = true
                    }
                }
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("androidHostTest").dependencies {
                    implementation(libs.library("roborazzi"))
                    implementation(libs.library("roborazzi-compose"))
                    implementation(libs.library("robolectric"))
                    implementation(libs.library("junit"))
                    implementation(libs.library("compose-ui-test-junit4"))
                    implementation(project(":core:screenshot-testing:public"))
                }
            }
        }
    }
}
