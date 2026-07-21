package org.example.project.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.client.feature.notes.impl.Res
import org.example.project.client.feature.notes.impl.error_notes_quota_exceeded
import org.example.project.core.ui.error.message
import org.jetbrains.compose.resources.stringResource

@ContributesBinding(AppScope::class)
@Inject
class DefaultNotesScreen : NotesScreen {
    @Composable
    override fun Content(component: NotesComponent) {
        val state by component.state.collectAsStateWithLifecycle()

        NotesScreenContent(state = state)
    }
}

@Composable
internal fun NotesScreenContent(state: NotesComponent.State) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Notes", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = state.textInput,
            onValueChange = { state.eventSink(NotesComponent.Event.TextChanged(it)) },
            label = { Text("Write a note…") },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        // The Declared quota failure shows its budget inline; anything else rides the generic
        // renderer via error.message().
        val message =
            state.quotaExceeded?.let { quota ->
                stringResource(Res.string.error_notes_quota_exceeded, quota.used, quota.quota)
            } ?: state.error?.message()
        message?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { state.eventSink(NotesComponent.Event.AddClicked) },
            enabled = !state.isSubmitting && state.textInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add note")
        }

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else if (state.notes.isEmpty()) {
            Text(
                text = "No notes yet — add your first above.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onDelete = { state.eventSink(NotesComponent.Event.DeleteClicked(note.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The author email is resolved server-side via the cross-domain call into
                // AuthService.
                Text(
                    text = note.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
