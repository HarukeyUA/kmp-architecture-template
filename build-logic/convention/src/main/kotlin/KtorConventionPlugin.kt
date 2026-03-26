import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KtorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.library("ktor-client-core"))
                        implementation(libs.library("ktor-client-content-negotiation"))
                        implementation(libs.library("ktor-serialization-kotlinx-json"))
                        implementation(libs.library("ktor-client-logging"))
                        implementation(libs.library("ktor-client-auth"))
                    }
                    jvmMain.dependencies { implementation(libs.library("ktor-client-okhttp")) }
                    androidMain.dependencies { implementation(libs.library("ktor-client-okhttp")) }
                    iosMain.dependencies { implementation(libs.library("ktor-client-darwin")) }
                }
            }
        }
    }
}
