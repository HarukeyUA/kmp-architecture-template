package org.example.project.feature.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.time.Instant
import org.example.project.client.feature.tasks.impl.Res
import org.example.project.client.feature.tasks.impl.ic_add
import org.example.project.client.feature.tasks.impl.ic_refresh
import org.example.project.client.feature.tasks.impl.tasks_add
import org.example.project.client.feature.tasks.impl.tasks_empty_body
import org.example.project.client.feature.tasks.impl.tasks_empty_title
import org.example.project.client.feature.tasks.impl.tasks_failed_title
import org.example.project.client.feature.tasks.impl.tasks_refresh
import org.example.project.client.feature.tasks.impl.tasks_retry
import org.example.project.client.feature.tasks.impl.tasks_title
import org.example.project.client.feature.tasks.impl.tasks_toggle_done
import org.example.project.core.ui.error.message
import org.example.project.core.ui.layout.AppListScaffold
import org.example.project.core.ui.theme.AppTheme
import org.example.project.feature.tasks.TaskListComponent.Event
import org.example.project.feature.tasks.TaskListComponent.State
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TaskListPane(state: State, modifier: Modifier = Modifier) {
    val eventSink = (state as? State.Loaded)?.eventSink
    AppListScaffold(
        title = stringResource(Res.string.tasks_title),
        modifier = modifier.testTag(TasksScreenTestTags.LIST),
        contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.Top,
        actions = {
            if (eventSink != null) {
                IconButton(
                    onClick = { eventSink(Event.RefreshClicked) },
                    modifier = Modifier.testTag(TasksScreenTestTags.REFRESH),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_refresh),
                        contentDescription = stringResource(Res.string.tasks_refresh),
                    )
                }
            }
        },
        floatingActionButton = {
            if (eventSink != null) {
                FloatingActionButton(
                    onClick = { eventSink(Event.AddClicked) },
                    modifier = Modifier.testTag(TasksScreenTestTags.ADD),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = stringResource(Res.string.tasks_add),
                    )
                }
            }
        },
    ) {
        when (state) {
            State.Loading -> centered { CircularProgressIndicator() }
            is State.Empty -> centered { EmptyMessage() }
            is State.Failed -> centered { FailedMessage(state = state) }
            is State.Content -> taskRows(state)
        }
    }
}

private fun LazyListScope.centered(content: @Composable () -> Unit) {
    item {
        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun EmptyMessage() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp).testTag(TasksScreenTestTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.tasks_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.tasks_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FailedMessage(state: State.Failed) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.tasks_failed_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = state.error.message(),
            modifier = Modifier.testTag(TasksScreenTestTags.ERROR),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = { state.eventSink(Event.RefreshClicked) },
            modifier = Modifier.testTag(TasksScreenTestTags.RETRY),
        ) {
            Text(text = stringResource(Res.string.tasks_retry))
        }
    }
}

private fun LazyListScope.taskRows(state: State.Content) {
    if (state.isRefreshing) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
    }
    state.error?.let { error ->
        item {
            ErrorBanner(
                error = error,
                onDismiss = { state.eventSink(Event.ErrorDismissed) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    items(items = state.tasks, key = { it.id }) { task ->
        TaskRow(
            task = task,
            isSelected = task.id == state.selectedTaskId,
            isUpdating = task.id in state.updatingTaskIds,
            onClick = { state.eventSink(Event.TaskClicked(task.id)) },
            onDoneChange = { state.eventSink(Event.DoneToggled(task.id, it)) },
        )
    }
}

@Composable
private fun TaskRow(
    task: Task,
    isSelected: Boolean,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onDoneChange: (Boolean) -> Unit,
) {
    val toggleLabel = stringResource(Res.string.tasks_toggle_done, task.title)
    ListItem(
        modifier =
            Modifier.fillMaxWidth()
                .testTag(TasksScreenTestTags.row(task.title))
                .clickable(onClick = onClick),
        colors =
            if (isSelected) {
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            } else {
                ListItemDefaults.colors()
            },
        leadingContent = {
            Checkbox(
                checked = task.done,
                onCheckedChange = onDoneChange,
                enabled = !isUpdating,
                modifier =
                    Modifier.testTag(TasksScreenTestTags.rowDone(task.title)).semantics {
                        contentDescription = toggleLabel
                    },
            )
        },
        headlineContent = {
            Text(
                text = task.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
            )
        },
    )
}

// The last row must scroll clear of the floating button.
private val FAB_CLEARANCE = 88.dp

internal val previewTasks =
    listOf(
        Task(
            id = "1",
            title = "Book the dentist",
            done = false,
            createdAt = Instant.fromEpochSeconds(1_780_000_000),
        ),
        Task(
            id = "2",
            title = "Renew the passport before the summer trip",
            done = true,
            createdAt = Instant.fromEpochSeconds(1_779_000_000),
        ),
        Task(
            id = "3",
            title = "Call the plumber",
            done = false,
            createdAt = Instant.fromEpochSeconds(1_778_000_000),
        ),
    )

@Preview
@Composable
private fun TaskListPanePreview() {
    AppTheme {
        TaskListPane(
            state =
                State.Content(
                    tasks = previewTasks,
                    selectedTaskId = null,
                    updatingTaskIds = emptySet(),
                    isRefreshing = false,
                    error = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview
@Composable
private fun TaskListPaneEmptyPreview() {
    AppTheme { TaskListPane(state = State.Empty(eventSink = {})) }
}
