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
        register("robotsAggregatorSettings") {
            id = "convention.robots-aggregator.settings"
            implementationClass = "org.example.project.RobotsAggregatorSettingsPlugin"
        }
        register("moduleStructureAssertSettings") {
            id = "convention.module-structure-assert.settings"
            implementationClass = "org.example.project.ModuleStructureAssertSettingsPlugin"
        }
    }
}
