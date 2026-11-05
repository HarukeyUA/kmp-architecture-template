package org.example.project.core.local.storage

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Buffer
import org.example.project.core.local.storage.model.SettingsLocalModel
import org.example.project.core.local.storage.model.ThemeMode

/**
 * Pins the bytes of `settings.json`. `readFrom` answers a value on any decoding failure, so without
 * this test a drift in the persisted shape would silently reset settings.
 */
@OptIn(ExperimentalSerializationApi::class)
class DataStoreJsonSerializerTest {
    @Test
    fun writeTo_producesTheGoldenPayload() = runTest {
        val sink = Buffer()

        serializer.writeTo(SETTINGS, sink)

        assertThat(sink.readUtf8()).isEqualTo(GOLDEN_PAYLOAD)
    }

    @Test
    fun readFrom_goldenPayload_decodesUnchanged() = runTest {
        val source = Buffer().writeUtf8(GOLDEN_PAYLOAD)

        assertThat(serializer.readFrom(source)).isEqualTo(SETTINGS)
    }

    @Test
    fun readFrom_unparseablePayload_answersTheUnreadableValue() = runTest {
        val source = Buffer().writeUtf8("not json")

        assertThat(serializer.readFrom(source)).isEqualTo(UNREADABLE)
    }

    private companion object {
        val SETTINGS = SettingsLocalModel(themeMode = ThemeMode.Dark)
        val UNREADABLE = SettingsLocalModel(themeMode = ThemeMode.Light)

        val serializer =
            dataStoreJsonSerializer(
                serializer = SettingsLocalModel.serializer(),
                defaultValue = SettingsLocalModel(),
                unreadableValue = UNREADABLE,
            )

        const val GOLDEN_PAYLOAD = """{"themeMode":"Dark"}"""
    }
}
