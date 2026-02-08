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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "MyApplication"

// App modules
include(":androidApp")

include(":composeApp")

// Core modules
include(":core:component:public")

include(":core:component:impl")

include(":core:navigation:public")

include(":core:ui:public")

include(":core:local-storage:public")

include(":core:local-storage:impl")

// Feature modules
include(":feature:auth:public")

include(":feature:auth:impl")

include(":feature:main:public")

include(":feature:main:impl")

include(":feature:home:public")

include(":feature:home:impl")

include(":feature:search:public")

include(":feature:search:impl")

include(":feature:profile:public")

include(":feature:profile:impl")

include(":feature:user-data:public")

include(":feature:user-data:impl")

include(":feature:user-data:testing")

include(":core:testing:public")
