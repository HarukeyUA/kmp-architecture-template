package org.example.project.server.database

import org.jetbrains.exposed.v1.core.Table

/**
 * The Exposed [Table]s a domain owns. Each domain's `:impl` contributes one via
 * `@ContributesIntoSet(AppScope::class)`; the migration drift test collects the whole
 * `Set<TableSet>` to learn the code-side schema — so there is no central `Schema.kt` and adding a
 * domain's tables touches zero shared files (ADR-0007).
 */
class TableSet(val tables: List<Table>) {
    constructor(vararg tables: Table) : this(tables.toList())
}
