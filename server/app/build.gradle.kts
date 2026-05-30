plugins { alias(libs.plugins.convention.server.app) }

// All `:server:*:impl` modules are wired in automatically by convention.impl-aggregator, so Metro
// can merge their @ContributesIntoSet route/table/error contributions into the server graph.
// Adding a domain touches zero lines here.

dependencies {
    // Migration drift test: diff the aggregated Exposed schema (graph.tableSets) against a
    // Testcontainers Postgres with all migrations applied. Lives here because only the app graph
    // assembles every domain's tables (ADR-0007).
    testImplementation(libs.exposed.migration.jdbc)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
}
