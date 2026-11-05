import org.example.project.CORE_ROBOTS_MODULE
import org.example.project.configureKmpUiTestDriver
import org.example.project.siblingImplModule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** `:robots` UI-test driver modules; the sibling `:impl` supplies the test tags. */
class KmpFeatureRobotsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureKmpUiTestDriver()
            if (path == CORE_ROBOTS_MODULE) return

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain.dependencies { implementation(project(siblingImplModule())) }
                }
            }
        }
    }
}
