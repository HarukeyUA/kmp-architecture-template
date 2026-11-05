package org.example.project.server.database

import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

/**
 * An [Instant] column backed by `TIMESTAMPTZ` — the only timestamp column type here.
 *
 * Exposed's plain `timestamp()` converts through `TimeZone.currentSystemDefault()` and stores
 * wall-clock digits in `TIMESTAMP WITHOUT TIME ZONE`, so two nodes in different timezones disagree
 * on every stored instant, a bug that stays silent until a second node runs. `TIMESTAMPTZ` stores a
 * true point in time regardless of JVM or connection timezone; the offset written is always UTC and
 * the offset read back is discarded, so no timezone ever reaches the domain.
 */
fun Table.utcTimestamp(name: String): Column<Instant> =
    timestampWithTimeZone(name)
        .transform(
            wrap = { it.toInstant().toKotlinInstant() },
            unwrap = { it.toJavaInstant().atOffset(ZoneOffset.UTC) },
        )
