import org.example.project.configureKmpUiTestDriver
import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * The `:client:e2e` driver module: the journeys above the robots, the per-host `E2eScope` graphs,
 * and every `:robots` module as `api` so a test host depends on this module alone. `androidMain`
 * reaches the Activity lifecycle through `androidx.test:core`, hence a main-source dep.
 */
class KmpE2eConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureKmpUiTestDriver()
            pluginManager.apply("convention.robots-aggregator")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    androidMain.dependencies {
                        api(libs.library("androidx-test-core"))
                        api(libs.library("androidx-activity"))
                    }
                }
            }
        }
    }
}
