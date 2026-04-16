plugins { `kotlin-dsl` }

dependencies { implementation(libs.spotless.gradlePlugin) }

gradlePlugin {
    plugins {
        register("implAggregatorSettings") {
            id = "convention.impl-aggregator.settings"
            implementationClass = "org.example.project.ImplAggregatorSettingsPlugin"
        }
        register("spotless") {
            id = "convention.spotless"
            implementationClass = "SpotlessConventionPlugin"
        }
    }
}
