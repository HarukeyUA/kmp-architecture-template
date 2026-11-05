package org.example.project.core.screenshot.testing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import assertk.assertThat
import assertk.assertions.isLessThan
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test

/**
 * Guards the harness itself. A dialog draws into a window of its own, and a capture that reuses one
 * window across both themes files the light image under the dark name without failing anything.
 */
class DialogThemeScreenshotTest : ScreenshotTest() {

    @Test
    fun dialogTheme() {
        capture {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Text(text = "Behind the scrim", modifier = Modifier.padding(32.dp))
            }
            Dialog(onDismissRequest = {}) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Text(text = "In the dialog", modifier = Modifier.padding(32.dp))
                }
            }
        }

        listOf("phone", "tablet").forEach { device ->
            assertThat(meanLuminance(device, theme = "dark"), name = "$device dark")
                .isLessThan(meanLuminance(device, theme = "light"))
        }
    }

    // Averaged over the whole capture: 0f for an all-black image, 255f for an all-white one.
    private fun meanLuminance(device: String, theme: String): Float {
        val image =
            ImageIO.read(File("src/androidHostTest/screenshots/dialogTheme_${device}_$theme.png"))
        var total = 0.0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y)
                total += ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3.0
            }
        }
        return (total / (image.width * image.height)).toFloat()
    }
}
