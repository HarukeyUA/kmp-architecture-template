package org.example.project.core.database

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.vinceglb.filekit.FileKit
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import org.example.project.core.database.di.DatabaseGraph
import org.example.project.core.database.di.JvmDatabaseGraph

@OptIn(ExperimentalPathApi::class)
class JvmDatabaseGraphTest {
    private val directory = Files.createTempDirectory("jvm-database-graph-test")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `the database file goes under the FileKit folder`() = runTest {
        FileKit.init(appId = "database-test", filesDir = directory.toFile())
        val database =
            DatabaseGraph.provideAppDatabase(
                JvmDatabaseGraph.provideAppDatabaseBuilder(),
                Dispatchers.IO,
            )

        try {
            database.insertRow(1)

            assertThat(database.rowCount()).isEqualTo(1)
            assertThat(directory.resolve(DATABASE_FILE_NAME).exists()).isTrue()
        } finally {
            database.close()
        }
    }
}
