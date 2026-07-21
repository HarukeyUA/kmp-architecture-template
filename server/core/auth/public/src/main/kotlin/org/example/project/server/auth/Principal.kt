package org.example.project.server.auth

import java.util.UUID

/** A server-side account identifier. */
@JvmInline value class AccountId(val value: UUID)

/**
 * The authenticated identity attached to a request once a Session is validated (CONTEXT.md). Plain
 * data class — Ktor 3's Authentication accepts any type as the call principal.
 */
data class Principal(val accountId: AccountId)
