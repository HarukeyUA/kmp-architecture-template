plugins { alias(libs.plugins.convention.server.core.public) }

dependencies {
    // TableSet exposes Exposed Table; the transaction helper exposes JdbcTransaction as its
    // receiver — both are part of this module's public API surface.
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
}
