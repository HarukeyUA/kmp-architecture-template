plugins { alias(libs.plugins.convention.server.feature.public) }

dependencies {
    // The NotesService contract speaks the seam's DTOs/errors and the invariant Principal identity.
    api(project(":shared:notes"))
    api(project(":server:core:auth:public"))
}
