package org.example.project.shared.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

/**
 * Builds the seam's [Json] from the cross-cutting [commonApiErrorSerializersModule] folded with the
 * given per-domain modules. **Client and server both call this** with their multibound
 * `Set<SerializersModule>`, so the wire format is identical on both ends.
 *
 * Forward-compatible by default — `ignoreUnknownKeys` and `explicitNulls = false` let an old client
 * tolerate new fields a newer server sends.
 */
fun buildSeamJson(domainModules: Set<SerializersModule> = emptySet()): Json {
    val combined =
        domainModules.fold(commonApiErrorSerializersModule) { accumulated, module ->
            accumulated + module
        }
    return Json {
        serializersModule = combined
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
}
