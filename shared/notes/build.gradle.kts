plugins { alias(libs.plugins.convention.shared.contract) }

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":shared:common")) }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
        }
        // The declared-set freeze (ADR-0011) reaches each lens via KClass.sealedSubclasses —
        // JVM-only reflection, so that test lives in jvmTest with kotlin-reflect on the classpath.
        jvmTest.dependencies { implementation(libs.kotlin.reflect) }
    }
}
