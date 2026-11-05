package org.example.project.core.local.storage

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import okio.FileSystem
import okio.Path.Companion.toOkioPath

@ContributesTo(AppScope::class)
interface AndroidDataStoreStorageGraph {
    @Provides
    fun provideDataStoreDirectory(context: Context): DataStoreDirectory =
        DataStoreDirectory(FileSystem.SYSTEM) { context.filesDir.toOkioPath() }
}
