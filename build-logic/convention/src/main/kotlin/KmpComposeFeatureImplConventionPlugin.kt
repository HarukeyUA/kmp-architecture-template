import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
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
                sourceSets.commonMain.dependencies {
                    implementation(project(":client:core:component:public"))
                    implementation(project(":client:core:ui:public"))
                }
            }
        }
    }
}
