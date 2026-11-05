plugins { alias(libs.plugins.convention.kmp.feature.impl) }

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(libs.ksafe) }
    }
}
