import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.findPlugin("detekt").get().get().pluginId)

            extensions.configure<DetektExtension> {
                toolVersion.set(libs.findVersion("detekt").get().requiredVersion)
                config.setFrom(rootDir.resolve("config/detekt/detekt.yml"))
                buildUponDefaultConfig.set(true)
                parallel.set(true)
                basePath.set(layout.projectDirectory)
                failOnSeverity.set(FailOnSeverity.Warning)
                source.setFrom(layout.projectDirectory.dir("src"))
            }

            tasks.withType<Detekt>().configureEach {
                include("**/*.kt", "**/*.kts")
                exclude("**/.gradle/**", "**/.kotlin/**", "**/build/**", "**/iosApp/**")

                reports {
                    html.required.set(true)
                    checkstyle.required.set(true)
                    sarif.required.set(true)
                    markdown.required.set(true)
                }
            }
        }
    }
}
