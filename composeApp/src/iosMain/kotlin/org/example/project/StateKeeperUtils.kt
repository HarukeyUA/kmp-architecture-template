package org.example.project

import com.arkivanov.essenty.statekeeper.SerializableContainer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSCoder
import platform.Foundation.NSString
import platform.Foundation.decodeTopLevelObjectOfClass
import platform.Foundation.encodeObject

private const val STATE_KEY = "org.example.project.state"

private val json =
    Json {
        allowStructuredMapKeys = true
    }

@Suppress("unused")
fun save(coder: NSCoder, state: SerializableContainer) {
    coder.encodeObject(
        `object` = json.encodeToString(SerializableContainer.serializer(), state),
        forKey = STATE_KEY
    )
}

@Suppress("unused")
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun restore(coder: NSCoder): SerializableContainer? =
    (coder.decodeTopLevelObjectOfClass(
        aClass = NSString,
        forKey = STATE_KEY,
        error = null
    ) as String?)?.let {
        try {
            json.decodeFromString(SerializableContainer.serializer(), it)
        } catch (e: Exception) {
            null
        }
    }