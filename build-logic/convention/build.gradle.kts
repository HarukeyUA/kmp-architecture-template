plugins { `kotlin-dsl` }

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.serialization.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "convention.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpFeaturePublic") {
            id = "convention.kmp.feature.public"
            implementationClass = "KmpFeaturePublicConventionPlugin"
        }
        register("kmpComposeFeaturePublic") {
            id = "convention.kmp.compose.feature.public"
            implementationClass = "KmpComposeFeaturePublicConventionPlugin"
        }
        register("kmpComposeFeatureImpl") {
            id = "convention.kmp.compose.feature.impl"
            implementationClass = "KmpComposeFeatureImplConventionPlugin"
        }
        register("kmpFeatureImpl") {
            id = "convention.kmp.feature.impl"
            implementationClass = "KmpFeatureImplConventionPlugin"
        }
        register("metro") {
            id = "convention.metro"
            implementationClass = "MetroConventionPlugin"
        }
        register("molecule") {
            id = "convention.molecule"
            implementationClass = "MoleculeConventionPlugin"
        }
        register("compose") {
            id = "convention.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("composeResources") {
            id = "convention.compose.resources"
            implementationClass = "ComposeResourcesConventionPlugin"
        }
        register("coroutines") {
            id = "convention.coroutines"
            implementationClass = "CoroutinesConventionPlugin"
        }
        register("decompose") {
            id = "convention.decompose"
            implementationClass = "DecomposeConventionPlugin"
        }
        register("serialization") {
            id = "convention.serialization"
            implementationClass = "SerializationConventionPlugin"
        }
        register("arrow") {
            id = "convention.arrow"
            implementationClass = "ArrowConventionPlugin"
        }
        register("ktor") {
            id = "convention.ktor"
            implementationClass = "KtorConventionPlugin"
        }
        register("screenshotTesting") {
            id = "convention.screenshot.testing"
            implementationClass = "ScreenshotTestingConventionPlugin"
        }
        register("implAggregator") {
            id = "convention.impl-aggregator"
            implementationClass = "ImplAggregatorConventionPlugin"
        }
        register("moduleGraphAssert") {
            id = "convention.module-graph-assert"
            implementationClass = "ModuleGraphAssertConventionPlugin"
        }
        register("detekt") {
            id = "convention.detekt"
            implementationClass = "DetektConventionPlugin"
        }
    }
}
