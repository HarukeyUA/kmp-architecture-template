import androidx.room3.gradle.RoomExtension
import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetSupportException

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // KSP's iOS tasks throw when created on a host Kotlin/Native does not support, and a
            // linux/arm64 server image configures this module without ever building it.
            if (!isKotlinNativeHost()) return

            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("androidx.room3")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.library("room-runtime"))
                        implementation(libs.library("sqlite-bundled"))
                    }
                }
            }

            listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64", "kspJvm").forEach {
                configuration ->
                dependencies.add(configuration, libs.library("room-compiler"))
            }

            extensions.configure<RoomExtension> { schemaDirectory("$projectDir/schemas") }
        }
    }
}

private fun isKotlinNativeHost(): Boolean =
    try {
        HostManager.host
        true
    } catch (_: TargetSupportException) {
        false
    }
