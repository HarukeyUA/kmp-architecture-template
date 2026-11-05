import org.example.project.androidLibrary
import org.example.project.jdkJvmTarget
import org.example.project.libs
import org.example.project.namespace
import org.example.project.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
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
                compilerOptions {
                    freeCompilerArgs.addAll(
                        "-Xexpect-actual-classes",
                        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    )
                }

                iosArm64()
                iosSimulatorArm64()
                jvm { compilerOptions { jvmTarget.set(jdkJvmTarget()) } }

                androidLibrary {
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()
                    namespace = namespace()

                    compilerOptions { jvmTarget.set(jdkJvmTarget()) }
                }
            }

            // Prevent test JVM forks from registering as foreground apps on macOS
            // (Skiko/Compose UI tests initialize AWT/Cocoa and would otherwise show
            // in the dock and steal focus).
            tasks.withType<Test>().configureEach {
                systemProperty("java.awt.headless", "true")
                systemProperty("apple.awt.UIElement", "true")
                // FFM restricted methods warn on JDK 25 without this grant and will refuse to run
                // in a future JDK.
                jvmArgs("--enable-native-access=ALL-UNNAMED")
            }
        }
    }
}
