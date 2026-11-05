plugins { alias(libs.plugins.convention.server.feature.public) }

dependencies {
    api(project(":shared:tasks"))
    api(project(":server:core:auth:public"))
    api(project(":server:core:web:public"))
}
