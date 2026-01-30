import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class DecomposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("decompose"))
                    implementation(libs.library("decompose-compose"))
                    implementation(libs.library("essenty-lifecycle"))
                    implementation(libs.library("essenty-statekeeper"))
                    implementation(libs.library("essenty-lifecycle-coroutines"))
                }
            }
        }
    }
}
