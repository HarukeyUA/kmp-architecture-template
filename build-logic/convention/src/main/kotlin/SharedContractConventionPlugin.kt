import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A `:shared:*` Seam contract module — pure wire types (DTOs, `@Resource` routes, shape validation,
 * the `ApiError` taxonomy) shared by client and server. Built on the client's KMP base so it
 * compiles for Android / iOS / JVM, but with a deliberately **rationed** dependency surface: only
 * kotlinx.serialization, ktor-resources, arrow-core, kotlinx.datetime.
 */
class SharedContractConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("convention.kmp.library")
                apply("convention.serialization")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain.dependencies {
                        api(libs.library("kotlinx-serialization-json"))
                        api(libs.library("ktor-resources"))
                        api(libs.library("arrow-core"))
                        api(libs.library("kotlinx-datetime"))
                    }
                }
            }
        }
    }
}
