plugins { alias(libs.plugins.convention.kmp.feature.impl) }

kotlin {
    sourceSets {
        // KSafe persists a @Serializable blob in the platform's OS-backed secure store
        // (Keychain / Android Keystore / encrypted file) — never plain DataStore (ADR-0009).
        commonMain.dependencies { implementation(libs.ksafe) }
    }
}
