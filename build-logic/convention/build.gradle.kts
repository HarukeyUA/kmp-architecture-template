plugins { `kotlin-dsl` }

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.serialization.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "org.example.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpFeaturePublic") {
            id = "org.example.kmp.feature.public"
            implementationClass = "KmpFeaturePublicConventionPlugin"
        }
        register("kmpComposeFeaturePublic") {
            id = "org.example.kmp.compose.feature.public"
            implementationClass = "KmpComposeFeaturePublicConventionPlugin"
        }
        register("kmpComposeFeatureImpl") {
            id = "org.example.kmp.compose.feature.impl"
            implementationClass = "KmpComposeFeatureImplConventionPlugin"
        }
        register("kmpFeatureImpl") {
            id = "org.example.kmp.feature.impl"
            implementationClass = "KmpFeatureImplConventionPlugin"
        }
        register("metro") {
            id = "org.example.metro"
            implementationClass = "MetroConventionPlugin"
        }
        register("molecule") {
            id = "org.example.molecule"
            implementationClass = "MoleculeConventionPlugin"
        }
        register("compose") {
            id = "org.example.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("composeResources") {
            id = "org.example.compose.resources"
            implementationClass = "ComposeResourcesConventionPlugin"
        }
        register("coroutines") {
            id = "org.example.coroutines"
            implementationClass = "CoroutinesConventionPlugin"
        }
        register("decompose") {
            id = "org.example.decompose"
            implementationClass = "DecomposeConventionPlugin"
        }
        register("serialization") {
            id = "org.example.serialization"
            implementationClass = "SerializationConventionPlugin"
        }
        register("screenshotTesting") {
            id = "org.example.screenshot.testing"
            implementationClass = "ScreenshotTestingConventionPlugin"
        }
    }
}
