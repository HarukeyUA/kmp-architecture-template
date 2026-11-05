import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.kotlin.KtfmtStep.TrailingCommaManagementStrategy
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
            val isRoot = this == rootProject
            extensions.configure<SpotlessExtension> {
                kotlin {
                    ktfmt(ktfmtVersion).kotlinlangStyle().configure {
                        it.setTrailingCommaManagementStrategy(
                            TrailingCommaManagementStrategy.ONLY_ADD,
                        )
                        it.setContinuationIndent(4)
                        it.setMaxWidth(100)
                        it.setBlockIndent(4)
                    }
                    if (isRoot) {
                        target("src/**/*.kt", "build-logic/**/*.kt")
                    } else {
                        target("src/**/*.kt")
                    }
                }
                kotlinGradle {
                    ktfmt(ktfmtVersion).kotlinlangStyle()
                    if (isRoot) {
                        target("*.kts", "build-logic/**/*.kts")
                    } else {
                        target("*.kts")
                    }
                }
                format("xml") {
                    target("src/**/*.xml")
                    trimTrailingWhitespace()
                    endWithNewline()
                }
            }
        }
    }
}
