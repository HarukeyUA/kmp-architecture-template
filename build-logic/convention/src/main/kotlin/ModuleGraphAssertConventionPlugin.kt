import org.example.project.AssertModuleDependenciesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Registers `assertModuleDependencies` on the module it's applied to. The task inspects only this
 * project's own declared `project(...)` dependencies in main source-set configurations, so it works
 * under Gradle project isolation.
 *
 * Hooked into `check`. Applied from [KmpLibraryConventionPlugin] so every KMP module is covered.
 */
class ModuleGraphAssertConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val assertTask =
                tasks.register<AssertModuleDependenciesTask>("assertModuleDependencies") {
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    description =
                        "Assert declared project dependencies match ARCHITECTURE.md rules."
                    sourcePath.set(project.path)
                    stampFile.set(layout.buildDirectory.file("module-graph-assert/stamp.txt"))
                }

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
                kotlin.sourceSets.configureEach {
                    if (name.contains("Test", ignoreCase = true)) return@configureEach

                    val configNames =
                        listOf(
                            apiConfigurationName,
                            implementationConfigurationName,
                            compileOnlyConfigurationName,
                            runtimeOnlyConfigurationName,
                        )
                    configNames.forEach { cfgName ->
                        configurations.named(cfgName).configure {
                            val declared = dependencies
                            assertTask.configure {
                                dependencyPaths.addAll(
                                    provider {
                                        declared.withType(ProjectDependency::class.java).map {
                                            it.path
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { dependsOn(assertTask) }
        }
    }
}
