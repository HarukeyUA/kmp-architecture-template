package org.example.project.server.feature.auth

import org.example.project.server.auth.AccessToken
import org.example.project.server.auth.Session

/**
 * What a successful signup/login issues (ADR-0009 as amended): the short-lived JWT [accessToken]
 * for day-to-day requests, and the opaque [session] the client presents to refresh and logout.
 */
data class AuthTokens(val accessToken: AccessToken, val session: Session)
