import org.example.project.siblingPublicModule

plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.coroutines)
    alias(libs.plugins.convention.metro)
    alias(libs.plugins.convention.room)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(siblingPublicModule()))
            // Metro compiles the bindings into the aggregator's graph, where their Room types must
            // resolve.
            api(libs.room.runtime)
            implementation(project(":client:core:dispatchers:public"))
        }
        jvmMain.dependencies { implementation(libs.filekit.core) }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
