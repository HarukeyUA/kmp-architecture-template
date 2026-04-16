import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val spotlessPluginId = libs.findPlugin("spotless").get().get().pluginId
            val ktfmtVersion = libs.findVersion("ktfmt").get().requiredVersion

            pluginManager.apply(spotlessPluginId)
            extensions.configure<SpotlessExtension> {
                kotlin {
                    ktfmt(ktfmtVersion).kotlinlangStyle()
                    target("src/**/*.kt", "build-logic/**/*.kt")
                }
                kotlinGradle {
                    ktfmt(ktfmtVersion).kotlinlangStyle()
                    target("*.kts", "build-logic/**/*.kts")
                }
                format("xml") { target("src/**/*.xml") }
            }
        }
    }
}
