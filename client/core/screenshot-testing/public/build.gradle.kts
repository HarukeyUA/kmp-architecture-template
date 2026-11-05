plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.compose)
    alias(libs.plugins.roborazzi)
}

kotlin {
    // Only the harness's own self-test runs here; every other module gets this from
    // `convention.screenshot-testing`, which this module cannot apply to itself.
    android {
        withHostTest {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            implementation(libs.robolectric)
            implementation(libs.junit)
            implementation(libs.compose.ui.test.junit4)
            implementation(kotlin("test-junit"))
            // api, not implementation: `capture` takes ErrorRenderer in its signature.
            api(project(":client:core:ui:public"))
        }
        getByName("androidHostTest").dependencies { implementation(libs.assertk) }
    }
}
