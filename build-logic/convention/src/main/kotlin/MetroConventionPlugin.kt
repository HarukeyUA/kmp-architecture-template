import org.gradle.api.Plugin
import org.gradle.api.Project

class MetroConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("dev.zacsweers.metro")
            }
        }
    }
}
