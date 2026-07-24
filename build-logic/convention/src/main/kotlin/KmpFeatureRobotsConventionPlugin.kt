import org.example.project.androidLibrary
import org.example.project.library
import org.example.project.libs
import org.example.project.namespace
import org.example.project.siblingImplModule
import org.example.project.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `:robots` UI-test driver modules. Android + JVM targets only: the jvm target keeps a future
 * desktop E2E suite free, and no E2E runs on iOS. Robots are written against
 * `SemanticsNodeInteractionsProvider` (hence the `api` compose-ui-test surface) and depend on the
 * sibling `:impl`, where the test tags live next to the screens they mark, plus
 * `:client:core:robots`, which carries the shared `Robot`/`Wait` base every robot extends (as `api`
 * — the base class is part of each robot's constructor surface). `:client:core:robots` itself is
 * that base module and gets neither dependency.
 */
class KmpFeatureRobotsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
                apply("convention.spotless")
                apply("convention.detekt")
                apply("convention.module-graph-assert")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

                androidLibrary {
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()
                    namespace = namespace()

                    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
                }

                sourceSets {
                    commonMain.dependencies {
                        api(libs.library("compose-ui-test"))
                        if (path != CORE_ROBOTS_PATH) {
                            api(project(CORE_ROBOTS_PATH))
                            implementation(project(siblingImplModule()))
                        }
                    }
                }
            }
        }
    }

    private companion object {
        /** The shared robot base module — the one `:robots` leaf without a sibling `:impl`. */
        const val CORE_ROBOTS_PATH = ":client:core:robots"
    }
}
