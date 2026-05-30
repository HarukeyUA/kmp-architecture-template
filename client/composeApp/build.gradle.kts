plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.impl.aggregator)
    alias(libs.plugins.convention.serialization)
    alias(libs.plugins.convention.compose)
    alias(libs.plugins.convention.compose.resources)
    alias(libs.plugins.convention.molecule)
    alias(libs.plugins.convention.coroutines)
    alias(libs.plugins.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(libs.decompose)
            export(libs.essenty.lifecycle)
            export(libs.essenty.backhandler)
            export(libs.essenty.statekeeper)
            export(project(":client:core:component:public"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":client:core:ui:public"))

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
    }
}

dependencies { "androidRuntimeClasspath"(libs.compose.ui.tooling) }
