import org.example.project.siblingPublicModule

plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.coroutines)
    alias(libs.plugins.convention.serialization)
    alias(libs.plugins.convention.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(siblingPublicModule()))
            implementation(project(":client:core:dispatchers:public"))
            implementation(libs.kotlinx.serialization.json.okio)
            implementation(libs.androidx.datastore)
            implementation(libs.datastore.preferences)
        }
        jvmMain.dependencies { implementation(libs.filekit.core) }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
