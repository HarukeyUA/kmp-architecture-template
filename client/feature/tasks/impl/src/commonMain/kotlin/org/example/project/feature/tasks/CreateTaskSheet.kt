package org.example.project.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.client.feature.tasks.impl.Res
import org.example.project.client.feature.tasks.impl.tasks_create_cancel
import org.example.project.client.feature.tasks.impl.tasks_create_field
import org.example.project.client.feature.tasks.impl.tasks_create_submit
import org.example.project.client.feature.tasks.impl.tasks_create_title
import org.example.project.client.feature.tasks.impl.tasks_error_title_blank
import org.example.project.client.feature.tasks.impl.tasks_error_title_invalid
import org.example.project.client.feature.tasks.impl.tasks_error_title_too_long
import org.example.project.core.ui.layout.AdaptiveBottomSheetOrDialog
import org.example.project.core.ui.theme.AppTheme
import org.example.project.feature.tasks.CreateTaskComponent.Event
import org.example.project.feature.tasks.CreateTaskComponent.State
import org.example.project.feature.tasks.CreateTaskComponent.TitleError
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CreateTaskSheet(state: State) {
    val dismiss = { state.eventSink(Event.Dismissed) }
    AdaptiveBottomSheetOrDialog(onDismissRequest = dismiss) { animateClose ->
        LaunchedEffect(state.isCreated) { if (state.isCreated) animateClose(dismiss) }
        CreateTaskForm(state = state, onCancel = { animateClose(dismiss) })
    }
}

@Composable
internal fun CreateTaskForm(state: State, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.tasks_create_title),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            state = state.title,
            modifier = Modifier.fillMaxWidth().testTag(TasksScreenTestTags.CREATE_TITLE),
            label = { Text(text = stringResource(Res.string.tasks_create_field)) },
            isError = state.titleError != null,
            supportingText =
                state.titleError?.let { error -> { Text(text = stringResource(error.message())) } },
            lineLimits = TextFieldLineLimits.SingleLine,
            enabled = !state.isSubmitting,
        )
        state.error?.let { ErrorBanner(error = it) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(TasksScreenTestTags.CREATE_CANCEL),
            ) {
                Text(text = stringResource(Res.string.tasks_create_cancel))
            }
            Button(
                onClick = { state.eventSink(Event.SubmitClicked) },
                enabled = !state.isSubmitting,
                modifier = Modifier.testTag(TasksScreenTestTags.CREATE_SUBMIT),
            ) {
                Text(text = stringResource(Res.string.tasks_create_submit))
            }
        }
    }
}

private fun TitleError.message() =
    when (this) {
        TitleError.Blank -> Res.string.tasks_error_title_blank
        TitleError.TooLong -> Res.string.tasks_error_title_too_long
        TitleError.Invalid -> Res.string.tasks_error_title_invalid
    }

@Preview
@Composable
private fun CreateTaskFormPreview() {
    AppTheme {
        CreateTaskForm(
            state = State(title = rememberTextFieldState(), eventSink = {}),
            onCancel = {},
        )
    }
}
