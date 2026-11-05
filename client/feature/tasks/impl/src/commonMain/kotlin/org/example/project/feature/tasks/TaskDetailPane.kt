package org.example.project.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import org.example.project.client.feature.tasks.impl.Res
import org.example.project.client.feature.tasks.impl.ic_delete
import org.example.project.client.feature.tasks.impl.tasks_detail_back_to_list
import org.example.project.client.feature.tasks.impl.tasks_detail_created
import org.example.project.client.feature.tasks.impl.tasks_detail_delete
import org.example.project.client.feature.tasks.impl.tasks_detail_done
import org.example.project.client.feature.tasks.impl.tasks_detail_missing
import org.example.project.client.feature.tasks.impl.tasks_detail_placeholder
import org.example.project.client.feature.tasks.impl.tasks_detail_title
import org.example.project.core.datetime.LocalizedDateTime
import org.example.project.core.ui.theme.AppTheme
import org.example.project.core.ui.topbar.AppTopBar
import org.example.project.feature.tasks.TaskDetailComponent.Event
import org.example.project.feature.tasks.TaskDetailComponent.State
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The detail pane. A null [state] is the two-pane layout with no task open. */
@Composable
internal fun TaskDetailPane(state: State?, showBack: Boolean, modifier: Modifier = Modifier) {
    val loaded = state as? State.Loaded
    Scaffold(
        modifier = modifier.testTag(TasksScreenTestTags.DETAIL),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = state?.let { stringResource(Res.string.tasks_detail_title) },
                onBackClick = backAction(state).takeIf { showBack },
                actions = {
                    if (loaded != null) {
                        IconButton(
                            onClick = { loaded.eventSink(Event.DeleteClicked) },
                            enabled = !loaded.isWriting,
                            modifier = Modifier.testTag(TasksScreenTestTags.DETAIL_DELETE),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_delete),
                                contentDescription = stringResource(Res.string.tasks_detail_delete),
                            )
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                null ->
                    Message(
                        text = stringResource(Res.string.tasks_detail_placeholder),
                        modifier = Modifier.align(Alignment.Center),
                    )
                State.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is State.Missing ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Message(text = stringResource(Res.string.tasks_detail_missing))
                        TextButton(onClick = { state.eventSink(Event.CloseClicked) }) {
                            Text(text = stringResource(Res.string.tasks_detail_back_to_list))
                        }
                    }
                is State.Loaded -> TaskDetails(state = state)
            }
        }
    }
}

private fun backAction(state: State?): (() -> Unit)? =
    when (state) {
        null,
        State.Loading -> null
        is State.Missing -> ({ state.eventSink(Event.CloseClicked) })
        is State.Loaded -> ({ state.eventSink(Event.CloseClicked) })
    }

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(24.dp).testTag(TasksScreenTestTags.DETAIL_PLACEHOLDER),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TaskDetails(state: State.Loaded) {
    val zone = TimeZone.currentSystemDefault()
    Column(
        modifier =
            Modifier.widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = state.task.title,
            modifier = Modifier.testTag(TasksScreenTestTags.DETAIL_TITLE),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text =
                stringResource(
                    Res.string.tasks_detail_created,
                    LocalizedDateTime.dateFull(state.task.createdAt, zone),
                    LocalizedDateTime.time(state.task.createdAt, zone),
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .testTag(TasksScreenTestTags.DETAIL_DONE)
                    .toggleable(
                        value = state.task.done,
                        enabled = !state.isWriting,
                        role = Role.Switch,
                        onValueChange = { state.eventSink(Event.DoneToggled(it)) },
                    )
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.tasks_detail_done),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(checked = state.task.done, onCheckedChange = null, enabled = !state.isWriting)
        }
        state.error?.let { ErrorBanner(error = it) }
    }
}

@Preview
@Composable
private fun TaskDetailPanePreview() {
    AppTheme {
        TaskDetailPane(
            state =
                State.Loaded(
                    task = previewTasks.first(),
                    isWriting = false,
                    error = null,
                    eventSink = {},
                ),
            showBack = true,
        )
    }
}

@Preview
@Composable
private fun TaskDetailPanePlaceholderPreview() {
    AppTheme { TaskDetailPane(state = null, showBack = false) }
}
