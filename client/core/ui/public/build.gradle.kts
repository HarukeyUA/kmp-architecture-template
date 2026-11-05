plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.compose)
    alias(libs.plugins.convention.compose.resources)
    alias(libs.plugins.convention.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.material3)
            api(libs.compose.material3.adaptive)
            api(libs.compose.resources)
            api(libs.decompose.compose)
            api(libs.decompose.compose.experimental)
            api(project(":client:core:navigation:public"))
            api(project(":client:core:error:public"))
            // Only ThemeMode is used, by the resolveDarkTheme() extension. Not `api`: that would
            // put SettingsLocalDataSource on the classpath of every Compose module undeclared.
            implementation(project(":client:core:local-storage:public"))
        }

        // Desktop-only theme integrations used by the JVM `actual`s of `SystemTheme.kt` and
        // `DynamicColor.jvm.kt`. Keep them off Android and iOS, which have their own platform APIs.
        jvmMain.dependencies {
            implementation(libs.nucleus.darkmode.detector)
            implementation(libs.nucleus.system.color)
            implementation(libs.material.kolor)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
        }

        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            // Skiko's native runtime, so `runComposeUiTest` has something to render into.
            implementation(compose.desktop.currentOs)
        }
    }
}
