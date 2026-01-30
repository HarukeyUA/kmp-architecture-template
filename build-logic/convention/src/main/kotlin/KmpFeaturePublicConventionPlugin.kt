import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpFeaturePublicConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.example.kmp.library")
                apply("org.example.coroutines")
                apply("org.example.serialization")
            }
        }
    }
}
