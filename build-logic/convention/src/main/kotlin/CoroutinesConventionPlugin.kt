import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CoroutinesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    androidMain.dependencies {
                        implementation(libs.library("kotlinx-coroutines-android"))
                    }

                    commonMain.dependencies {
                        implementation(libs.library("kotlinx-coroutines-core"))
                    }

                    jvmMain.dependencies {
                        implementation(libs.library("kotlinx-coroutines-swing"))
                    }
                }
            }
        }
    }
}
