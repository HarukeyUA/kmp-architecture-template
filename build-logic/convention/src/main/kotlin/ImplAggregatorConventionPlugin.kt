import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Applied to the aggregator KMP module (e.g. `:composeApp`). Reads the impl module list that
 * `convention.impl-aggregator.settings` wrote into this project's build directory at settings time
 * and wires each as an `api` dependency of `commonMain`.
 *
 * The file is resolved via `layout.projectDirectory`, which is scoped to this project, so no
 * cross-project access occurs. `providers.fileContents` registers the file as a configuration cache
 * input, so changes to the module list invalidate the cache correctly.
 *
 * The path must stay in sync with `ImplAggregatorSettingsPlugin.AGGREGATOR_OUTPUT_REL_PATH` in the
 * `:build-logic:settings` subproject. Kept separate to avoid a classpath dependency between the
 * settings and convention subprojects.
 */
class ImplAggregatorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val inputFile = layout.projectDirectory.file(AGGREGATOR_OUTPUT_REL_PATH)

            val modulesProvider: Provider<List<String>> =
                providers.fileContents(inputFile).asText.map { text ->
                    text.lines().map(String::trim).filter(String::isNotEmpty)
                }

            // KMP aggregator (e.g. :client:composeApp) wires impls into commonMain.
            plugins.withId("org.jetbrains.kotlin.multiplatform") {
                extensions.configure<KotlinMultiplatformExtension> {
                    sourceSets.commonMain.dependencies {
                        aggregatedModules(modulesProvider).forEach { path -> api(project(path)) }
                    }
                }
            }

            // Plain-JVM aggregator (e.g. :server:app) wires impls via the standard dependencies
            // block.
            plugins.withId("org.jetbrains.kotlin.jvm") {
                dependencies {
                    aggregatedModules(modulesProvider).forEach { path -> add("api", project(path)) }
                }
            }
        }
    }

    private fun aggregatedModules(provider: Provider<List<String>>): List<String> =
        provider.orNull
            ?: error(
                "Impl module list not found at `$AGGREGATOR_OUTPUT_REL_PATH`. " +
                    "Apply `convention.impl-aggregator.settings` in settings.gradle.kts " +
                    "and register the aggregator via `implAggregator { aggregator(...) }`."
            )

    private companion object {
        const val AGGREGATOR_OUTPUT_REL_PATH = "build/impl-aggregator/impl-modules.txt"
    }
}
