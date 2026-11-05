package org.example.project.feature.tasks

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.example.project.client.feature.tasks.impl.Res
import org.example.project.client.feature.tasks.impl.tasks_dismiss
import org.example.project.core.error.AppError
import org.example.project.core.ui.error.message
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ErrorBanner(
    error: AppError,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(TasksScreenTestTags.ERROR),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = error.message(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(Res.string.tasks_dismiss))
                }
            }
        }
    }
}
