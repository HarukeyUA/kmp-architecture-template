package org.example.project.feature.auth

/**
 * Semantics tags for the login screen, consumed by `:client:feature:auth:robots`. Declared next to
 * the screen they mark so a UI change and its tag change land in the same module.
 */
object LoginScreenTestTags {
    const val EMAIL = "login_email"
    const val PASSWORD = "login_password"
    const val LOG_IN = "login_log_in"
    const val CREATE_ACCOUNT = "login_create_account"
}
