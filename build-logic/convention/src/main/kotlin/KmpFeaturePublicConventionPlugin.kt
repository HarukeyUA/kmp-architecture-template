import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpFeaturePublicConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("convention.kmp.library")
                apply("convention.coroutines")
                apply("convention.serialization")
            }
        }
    }
}
