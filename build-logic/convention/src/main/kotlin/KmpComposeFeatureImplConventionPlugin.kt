import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.compose.ComposePlugin
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
                apply("convention.compose.resources")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain.dependencies {
                        implementation(project(":client:core:component:public"))
                        implementation(project(":client:core:ui:public"))
                    }
                    commonTest.dependencies { implementation(libs.library("compose-ui-test")) }
                    // Skiko's native runtime for the host OS. `runComposeUiTest` interaction
                    // suites belong in jvmTest (not commonTest) so the Android target stays free
                    // of emulator/Robolectric requirements.
                    jvmTest.dependencies {
                        implementation(ComposePlugin.Dependencies(target).desktop.currentOs)
                    }
                }
            }
        }
    }
}
