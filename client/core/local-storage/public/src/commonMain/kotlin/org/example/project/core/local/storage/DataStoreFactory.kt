package org.example.project.core.local.storage

import androidx.datastore.core.DataStore
import kotlinx.serialization.KSerializer

/** Opens JSON files in the app's persistent storage directory. */
interface DataStoreFactory {
    /**
     * The store over [fileName], reading [defaultValue] until first written and [unreadableValue]
     * when the file does not decode. Open each file once per process, as DataStore requires.
     */
    fun <T> create(
        fileName: String,
        serializer: KSerializer<T>,
        defaultValue: T,
        unreadableValue: T = defaultValue,
    ): DataStore<T>
}
