import org.example.project.AssertModuleDependenciesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

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
                assertTask.configure {
                    dependencyPaths.addAll(
                        provider {
                            kotlin.sourceSets
                                .asSequence()
                                .filterNot { it.name.contains("Test", ignoreCase = true) }
                                .flatMap { ss ->
                                    sequenceOf(
                                        ss.apiConfigurationName,
                                        ss.implementationConfigurationName,
                                        ss.compileOnlyConfigurationName,
                                        ss.runtimeOnlyConfigurationName,
                                    )
                                }
                                .mapNotNull { configurations.findByName(it) }
                                .flatMap {
                                    it.dependencies
                                        .withType(ProjectDependency::class.java)
                                        .asSequence()
                                        .map(ProjectDependency::getPath)
                                }
                                .toSet()
                        }
                    )
                }
            }

            // Plain-JVM server modules (`:server:*`) declare deps on the standard non-test main
            // configurations rather than KMP source sets.
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                assertTask.configure {
                    dependencyPaths.addAll(
                        provider {
                            sequenceOf("api", "implementation", "compileOnly", "runtimeOnly")
                                .mapNotNull { configurations.findByName(it) }
                                .flatMap {
                                    it.dependencies
                                        .withType(ProjectDependency::class.java)
                                        .asSequence()
                                        .map(ProjectDependency::getPath)
                                }
                                .toSet()
                        }
                    )
                }
            }

            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { dependsOn(assertTask) }
            tasks.withType<KotlinCompilationTask<*>>().configureEach { dependsOn(assertTask) }
        }
    }
}
