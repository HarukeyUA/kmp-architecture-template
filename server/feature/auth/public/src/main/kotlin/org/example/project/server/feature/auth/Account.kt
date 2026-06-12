package org.example.project.server.feature.auth

import org.example.project.server.auth.AccountId

/**
 * The auth domain's cross-module account model (ADR-0003/0006): what other domains and routes may
 * know about an account. Deliberately hash-free — credential material (the Argon2id hash) is
 * proof-of-identity owned by the Credential module's `:impl` and never leaves the login path.
 */
data class Account(val id: AccountId, val email: String)
