plugins {
    alias(libs.plugins.convention.kmp.feature.impl)
    alias(libs.plugins.convention.ktor)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":client:core:database:public"))
            implementation(project(":client:core:local-storage:public"))
            implementation(project(":client:core:network:public"))
            implementation(project(":shared:auth"))
            implementation(libs.androidx.datastore)
            implementation(libs.kermit)
            implementation(libs.ktor.client.resources)
        }
        commonTest.dependencies {
            implementation(project(":client:core:database:testing"))
            implementation(project(":client:feature:session:testing"))
            implementation(libs.ktor.client.mock)
        }
        // Runs the repository over the real factory, so a corrupt file is tested end to end.
        jvmTest.dependencies { implementation(project(":client:core:local-storage:impl")) }
    }
}
