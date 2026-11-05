package org.example.project.core.local.storage

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import okio.FileSystem
import okio.Path
import org.example.project.core.dispatchers.IoDispatcher

/** The platform's persistent folder, resolved on first read rather than at graph creation. */
class DataStoreDirectory(val fileSystem: FileSystem, val path: () -> Path)

@Inject
@ContributesBinding(AppScope::class)
class DefaultDataStoreFactory(
    private val directory: DataStoreDirectory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DataStoreFactory {
    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> create(
        fileName: String,
        serializer: KSerializer<T>,
        defaultValue: T,
        unreadableValue: T,
    ): DataStore<T> =
        DataStore.Builder(
                storage =
                    OkioStorage(
                        fileSystem = directory.fileSystem,
                        serializer =
                            dataStoreJsonSerializer(serializer, defaultValue, unreadableValue),
                        producePath = { directory.path().resolve(fileName) },
                    ),
                context = ioDispatcher + SupervisorJob(),
            )
            .build()
}
