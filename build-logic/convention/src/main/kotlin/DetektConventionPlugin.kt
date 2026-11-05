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
                basePath.set(layout.settingsDirectory)
                failOnSeverity.set(FailOnSeverity.Warning)
                source.setFrom(layout.projectDirectory.dir("src"))
            }

            tasks.withType<Detekt>().configureEach {
                include("**/*.kt", "**/*.kts")
                exclude("**/.gradle/**", "**/.kotlin/**", "**/build/**", "**/iosApp/**")
                // Compilation tasks source generated code by absolute root, so the pattern above
                // misses it.
                exclude { it.file.path.contains("/build/") }
                // Common source sets are already analysed with the JVM classpath; the Android
                // tasks only add the androidMain / androidHostTest files.
                when (name) {
                    "detektMainAndroid" -> exclude { it.file.path.contains("/src/commonMain/") }
                    "detektHostTestAndroid" -> exclude { it.file.path.contains("/src/commonTest/") }
                }

                reports {
                    html.required.set(true)
                    checkstyle.required.set(true)
                    sarif.required.set(true)
                    markdown.required.set(true)
                }
            }

            // The plain task has no classpath, so rules needing type resolution silently skip.
            // Chain the per-compilation tasks behind it so `./gradlew detekt` runs them all.
            tasks.named(DETEKT_TASK_NAME) {
                dependsOn(tasks.withType<Detekt>().matching { it.name in typedGateTasks() })
            }
        }
    }

    private fun Project.typedGateTasks(): Set<String> =
        if (pluginManager.hasPlugin("com.android.application")) ANDROID_APP_TYPED_TASKS
        else TYPED_TASKS

    private companion object {
        const val DETEKT_TASK_NAME = "detekt"

        val TYPED_TASKS =
            setOf(
                "detektMain",
                "detektTest",
                "detektMainJvm",
                "detektTestJvm",
                "detektMainAndroid",
                "detektHostTestAndroid",
            )

        val ANDROID_APP_TYPED_TASKS =
            setOf("detektDevDebug", "detektDevDebugUnitTest", "detektDevDebugAndroidTest")
    }
}
