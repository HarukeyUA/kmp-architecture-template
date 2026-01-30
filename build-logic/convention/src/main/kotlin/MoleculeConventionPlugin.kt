import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MoleculeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) { apply("org.jetbrains.kotlin.plugin.compose") }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies { implementation(libs.library("molecule")) }
            }
        }
    }
}
