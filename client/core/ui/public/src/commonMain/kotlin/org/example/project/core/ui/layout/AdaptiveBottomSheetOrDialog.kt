package org.example.project.core.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch
import org.example.project.core.ui.theme.AppTheme

/**
 * Modal popup rendered as a [ModalBottomSheet] on compact widths and an [AlertDialog] on medium and
 * expanded ones.
 *
 * Route every close action through the `animateClose` callback [content] receives, including
 * [onDismissRequest] (scrim, back or swipe). The sheet variant plays its hide animation before
 * running the action; removing the sheet from composition first makes it snap out with no
 * animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveBottomSheetOrDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    dialogContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    dialogShape: Shape = AlertDialogDefaults.shape,
    dialogMaxWidth: Dp = 360.dp,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable (animateClose: (() -> Unit) -> Unit) -> Unit,
) {
    val info = currentWindowAdaptiveInfoV2()
    val isExpandedWidth =
        info.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    if (isExpandedWidth) {
        AdaptiveDialog(
            onDismissRequest = onDismissRequest,
            containerColor = dialogContainerColor,
            shape = dialogShape,
            maxWidth = dialogMaxWidth,
            modifier = modifier,
            content = content,
        )
    } else {
        AdaptiveSheet(
            onDismissRequest = onDismissRequest,
            containerColor = sheetContainerColor,
            skipPartiallyExpanded = skipPartiallyExpanded,
            modifier = modifier,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdaptiveSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    skipPartiallyExpanded: Boolean,
    modifier: Modifier,
    content: @Composable (animateClose: (() -> Unit) -> Unit) -> Unit,
) {
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues =
                if (skipPartiallyExpanded) setOf(SheetValue.Hidden, SheetValue.Expanded)
                else setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded),
        )
    val scope = rememberCoroutineScope()
    val animateClose =
        remember(sheetState, scope) {
            { action: () -> Unit ->
                scope.launch {
                    sheetState.hide()
                    action()
                }
                Unit
            }
        }
    ModalBottomSheet(
        onDismissRequest = { animateClose(onDismissRequest) },
        sheetState = sheetState,
        modifier = modifier,
        containerColor = containerColor,
    ) {
        content(animateClose)
    }
}

@Composable
private fun AdaptiveDialog(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    shape: Shape,
    maxWidth: Dp,
    modifier: Modifier,
    content: @Composable (animateClose: (() -> Unit) -> Unit) -> Unit,
) {
    // Dialogs have no hide animation, so run the action immediately; the dialog leaves composition
    // once the action flips the parent's visibility flag.
    val animateClose: (() -> Unit) -> Unit = remember { { action -> action() } }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        confirmButton = {},
        dismissButton = null,
        title = null,
        text = {
            Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth()) {
                content(animateClose)
            }
        },
        containerColor = containerColor,
        shape = shape,
    )
}

@Preview
@Composable
private fun AdaptiveBottomSheetOrDialogPreview() {
    AppTheme {
        AdaptiveBottomSheetOrDialog(onDismissRequest = {}) { animateClose ->
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Delete this item?", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { animateClose {} }) { Text(text = "Cancel") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AdaptiveSheetPreview() {
    AppTheme {
        AdaptiveSheet(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            skipPartiallyExpanded = true,
            modifier = Modifier,
        ) { animateClose ->
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Delete this item?", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { animateClose {} }) { Text(text = "Cancel") }
            }
        }
    }
}

@Preview
@Composable
private fun AdaptiveDialogPreview() {
    AppTheme {
        AdaptiveDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = AlertDialogDefaults.shape,
            maxWidth = 360.dp,
            modifier = Modifier,
        ) { animateClose ->
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Delete this item?", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { animateClose {} }) { Text(text = "Cancel") }
            }
        }
    }
}
