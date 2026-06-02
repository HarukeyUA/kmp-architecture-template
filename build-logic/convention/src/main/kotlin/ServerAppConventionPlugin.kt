import org.example.project.configureServerKotlinJvm
import org.example.project.library
import org.example.project.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * The server composition root (`:server:app`): the Gradle `application` plugin + Metro graph + Ktor
 * engine. Like the client's `:client:composeApp`, it applies `convention.impl-aggregator` so every
 * `:server:*:impl` module is wired in automatically.
 */
class ServerAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("application")
                apply("convention.spotless")
                apply("convention.detekt")
                apply("convention.metro")
                apply("convention.module-graph-assert")
                apply("convention.impl-aggregator")
            }

            configureServerKotlinJvm()

            extensions.configure<JavaApplication> {
                applicationName = "server"
                mainClass.set("org.example.project.server.MainKt")
            }

            dependencies {
                add("implementation", libs.library("ktor-server-core"))
                add("implementation", libs.library("ktor-server-netty"))
                add("implementation", libs.library("ktor-server-content-negotiation"))
                add("implementation", libs.library("ktor-server-status-pages"))
                add("implementation", libs.library("ktor-server-call-id"))
                add("implementation", libs.library("ktor-server-call-logging"))
                add("implementation", libs.library("ktor-server-metrics-micrometer"))
                add("implementation", libs.library("ktor-server-resources"))
                add("implementation", libs.library("ktor-server-auth"))
                add("implementation", libs.library("ktor-serialization-kotlinx-json"))
                add("implementation", libs.library("kotlinx-serialization-json"))
                add("implementation", libs.library("micrometer-registry-prometheus"))
                add("implementation", libs.library("arrow-core"))
                add("implementation", libs.library("arrow-fx-coroutines"))
                add("implementation", libs.library("kotlinx-coroutines-core"))
                add("runtimeOnly", libs.library("logback-classic"))
                add("testImplementation", libs.library("ktor-server-test-host"))
                add("testImplementation", libs.library("kotlin-test"))
                add("testImplementation", libs.library("assertk"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
