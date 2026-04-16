plugins { `kotlin-dsl` }

gradlePlugin {
    plugins {
        register("implAggregatorSettings") {
            id = "convention.impl-aggregator.settings"
            implementationClass = "org.example.project.ImplAggregatorSettingsPlugin"
        }
    }
}
