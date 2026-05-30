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
 * kotlinx.serialization, ktor-resources, and arrow-core (plus kotlinx.datetime, added when a wire
 * type first needs a date). No Compose, no Ktor engines, no Exposed, no DataStore — that single
 * rule keeps the Seam from rotting into a god module (ADR-0001, ADR-0003).
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
                        // `api` because these types appear on the Seam's public surface
                        // (SerializersModule, JsonObject, Either, @Resource, Instant), which both
                        // sides consume.
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
