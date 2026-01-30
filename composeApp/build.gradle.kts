import org.example.project.addLocalImplDependencies
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.example.kmp.library)
    alias(libs.plugins.example.serialization)
    alias(libs.plugins.example.compose)
    alias(libs.plugins.example.compose.resources)
    alias(libs.plugins.example.molecule)
    alias(libs.plugins.example.coroutines)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(libs.decompose)
            export(libs.essenty.lifecycle)
            export(libs.essenty.backhandler)
            export(libs.essenty.statekeeper)
        }
    }

    sourceSets {
        commonMain.dependencies {
            addLocalImplDependencies(project)

            implementation(project(":core:ui:public"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.json.okio)

            implementation(libs.androidx.datastore)
            implementation(libs.datastore.preferences)

            api(libs.decompose)
            implementation(libs.decompose.compose)
            implementation(libs.decompose.compose.experimental)
            api(libs.essenty.lifecycle)
            api(libs.essenty.backhandler)
            api(libs.essenty.statekeeper)
            implementation(libs.essenty.lifecycle.coroutines)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

dependencies {
    "androidRuntimeClasspath"(libs.compose.ui.tooling)
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
