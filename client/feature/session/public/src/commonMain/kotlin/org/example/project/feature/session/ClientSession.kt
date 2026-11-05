package org.example.project.feature.session

import kotlinx.serialization.Serializable

/**
 * The signed-in state held on this device: the short-lived JWT [accessToken] sent as the bearer on
 * every request, and the opaque [refreshToken] presented only to the refresh and logout endpoints.
 */
@Serializable
data class ClientSession(val accessToken: String, val refreshToken: String) {
    // A session lands in logs through `$state` interpolation, and both fields are credentials.
    override fun toString(): String = "ClientSession(<redacted>)"
}
