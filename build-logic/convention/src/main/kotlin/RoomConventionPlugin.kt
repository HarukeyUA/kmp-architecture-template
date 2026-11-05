import androidx.room3.gradle.RoomExtension
import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
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
