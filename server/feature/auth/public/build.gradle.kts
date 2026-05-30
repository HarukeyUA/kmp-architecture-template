plugins { alias(libs.plugins.convention.server.feature.public) }

dependencies {
    // The AuthService contract speaks the seam's DTOs/errors and the invariant session/Principal
    // infra.
    api(project(":shared:auth"))
    api(project(":server:core:auth:public"))
}
