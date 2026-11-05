package org.example.project.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.client.feature.settings.impl.Res
import org.example.project.client.feature.settings.impl.settings_log_out
import org.example.project.client.feature.settings.impl.settings_log_out_failed
import org.example.project.client.feature.settings.impl.settings_theme_dark
import org.example.project.client.feature.settings.impl.settings_theme_light
import org.example.project.client.feature.settings.impl.settings_theme_section
import org.example.project.client.feature.settings.impl.settings_theme_system
import org.example.project.client.feature.settings.impl.settings_title
import org.example.project.core.local.storage.model.ThemeMode
import org.example.project.core.ui.layout.AppListScaffold
import org.example.project.core.ui.theme.AppTheme
import org.example.project.feature.settings.SettingsComponent.Event
import org.example.project.feature.settings.SettingsComponent.State
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Inject
@ContributesBinding(AppScope::class)
class DefaultSettingsScreen : SettingsScreen {
    @Composable
    override fun Content(component: SettingsComponent) {
        val state by component.state.collectAsStateWithLifecycle()
        SettingsScreenContent(state = state)
    }
}

@Composable
internal fun SettingsScreenContent(state: State, modifier: Modifier = Modifier) {
    AppListScaffold(title = stringResource(Res.string.settings_title), modifier = modifier) {
        item {
            Column(modifier = Modifier.selectableGroup()) {
                Text(
                    text = stringResource(Res.string.settings_theme_section),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                ThemeMode.entries.forEach { mode ->
                    ThemeRow(
                        mode = mode,
                        selected = mode == state.themeMode,
                        onClick = { state.eventSink(Event.ThemeModeSelected(mode)) },
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { state.eventSink(Event.LogoutClicked) },
                    enabled = !state.isLoggingOut,
                    modifier = Modifier.fillMaxWidth().testTag(SettingsScreenTestTags.LOG_OUT),
                ) {
                    Text(text = stringResource(Res.string.settings_log_out))
                }
                if (state.logoutFailed) {
                    Text(
                        text = stringResource(Res.string.settings_log_out_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .testTag(SettingsScreenTestTags.theme(mode))
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = stringResource(mode.label()), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun ThemeMode.label(): StringResource =
    when (this) {
        ThemeMode.System -> Res.string.settings_theme_system
        ThemeMode.Light -> Res.string.settings_theme_light
        ThemeMode.Dark -> Res.string.settings_theme_dark
    }

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme { SettingsScreenContent(state = State(themeMode = ThemeMode.Dark, eventSink = {})) }
}
