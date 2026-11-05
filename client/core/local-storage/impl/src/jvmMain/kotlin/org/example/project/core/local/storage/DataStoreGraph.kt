package org.example.project.core.local.storage

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import okio.FileSystem
import okio.Path.Companion.toPath

@ContributesTo(AppScope::class)
interface JvmDataStoreStorageGraph {
    @Provides
    fun provideDataStoreDirectory(): DataStoreDirectory =
        DataStoreDirectory(FileSystem.SYSTEM) { FileKit.filesDir.path.toPath() }
}
