import org.example.project.implAggregator
import org.example.project.robotsAggregator

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("convention.impl-aggregator.settings")
    id("convention.robots-aggregator.settings")
    id("convention.module-structure-assert.settings")
}

rootProject.name = "MyApplication"

implAggregator {
    aggregator(":client:composeApp")
    aggregator(":server:app")
}

// Wires every :client:*:robots module into the E2E host's androidTest configuration.
robotsAggregator { aggregator(":client:androidApp") }

// ===================== :client umbrella (Compose Multiplatform app) =====================
// App modules
include(":client:androidApp")

include(":client:desktopApp")

include(":client:composeApp")

// Core modules
include(":client:core:component:public")

include(":client:core:navigation:public")

include(":client:core:buildinfo:public")

include(":client:core:robots")

include(":client:core:error:public")

include(":client:core:network:public")

include(":client:core:network:impl")

include(":client:core:ui:public")

include(":client:core:dispatchers:public")

include(":client:core:dispatchers:impl")

include(":client:core:local-storage:public")

include(":client:core:local-storage:impl")

// Feature modules
include(":client:feature:auth:public")

include(":client:feature:auth:impl")

include(":client:feature:auth:robots")

include(":client:feature:main:public")

include(":client:feature:main:impl")

include(":client:feature:main:robots")

include(":client:feature:home:public")

include(":client:feature:home:impl")

include(":client:feature:notes:public")

include(":client:feature:notes:impl")

include(":client:feature:notes:robots")

include(":client:feature:splash:public")

include(":client:feature:splash:impl")

include(":client:feature:profile:public")

include(":client:feature:profile:impl")

include(":client:feature:user-data:public")

include(":client:feature:user-data:impl")

include(":client:feature:user-data:testing")

include(":client:core:testing:public")

include(":client:core:screenshot-testing:public")

include(":client:core:secure-storage:public")

include(":client:core:secure-storage:impl")

// ===================== :shared umbrella (the Seam — contracts) =====================
include(":shared:common")

include(":shared:auth")

include(":shared:notes")

// ===================== :server umbrella (Ktor server) =====================
include(":server:app")

include(":server:testing")

// Server core modules
include(":server:core:database:public")

include(":server:core:database:impl")

include(":server:core:lifecycle:public")

include(":server:core:web:public")

include(":server:core:web:impl")

include(":server:core:observability:public")

include(":server:core:observability:impl")

include(":server:core:auth:public")

include(":server:core:auth:impl")

include(":server:core:storage:public")

include(":server:core:storage:impl")

include(":server:core:storage:testing")

include(":server:core:scheduler:public")

include(":server:core:scheduler:impl")

// Server domain modules
include(":server:feature:auth:public")

include(":server:feature:auth:impl")

include(":server:feature:notes:public")

include(":server:feature:notes:impl")
