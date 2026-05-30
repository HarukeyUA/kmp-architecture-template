package org.example.project.server.database

import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.datetime.OffsetDateTimeColumnType

/**
 * Postgres `TIMESTAMP WITH TIME ZONE` mapped to [kotlin.time.Instant].
 *
 * Postgres `TIMESTAMPTZ` stores a UTC instant — the offset is not preserved across the DB boundary.
 * Exposed's stock binding is [java.time.OffsetDateTime], which advertises an offset that always
 * round-trips as `+00:00`. [Instant] is the honest type for that semantic; this wraps the existing
 * `OffsetDateTime` machinery and normalises every value to UTC at I/O. The SQL column type is
 * identical to Exposed's stock `timestampWithTimeZone(...)`, so it is a drop-in with no migration
 * difference (keeping the drift test happy).
 */
private class InstantWithTimeZoneColumnType : OffsetDateTimeColumnType<Instant>() {
    override fun toOffsetDateTime(value: Instant): OffsetDateTime =
        OffsetDateTime.ofInstant(value.toJavaInstant(), ZoneOffset.UTC)

    override fun fromOffsetDateTime(datetime: OffsetDateTime): Instant =
        datetime.toInstant().toKotlinInstant()
}

/** Declares a Postgres `TIMESTAMP WITH TIME ZONE` column whose Kotlin type is [Instant]. */
fun Table.instantTimestampTz(name: String): Column<Instant> =
    registerColumn(name, InstantWithTimeZoneColumnType())
