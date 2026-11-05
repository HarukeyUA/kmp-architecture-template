package org.example.project.feature.tasks

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class DefaultTasksScreen : TasksScreen {
    @Composable
    override fun Content(component: TasksComponent) {
        val state by component.state.collectAsStateWithLifecycle()
        AdaptiveTasksScreenContent(state = state)
    }
}

/** [TasksScreenContent] laid out for the current window. */
@Composable
internal fun AdaptiveTasksScreenContent(state: TasksComponent.State) {
    val isExpanded =
        currentWindowAdaptiveInfoV2()
            .windowSizeClass
            .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    TasksScreenContent(state = state, isExpanded = isExpanded)
}

@Composable
internal fun TasksScreenContent(
    state: TasksComponent.State,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    state.create?.let { CreateTaskSheet(state = it) }

    if (isExpanded) {
        Row(modifier = modifier.fillMaxSize()) {
            TaskListPane(state = state.list, modifier = Modifier.weight(LIST_PANE_WEIGHT))
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            TaskDetailPane(
                state = state.detail,
                showBack = false,
                modifier = Modifier.weight(1f - LIST_PANE_WEIGHT),
            )
        }
    } else {
        val detail = state.detail
        if (detail == null) {
            TaskListPane(state = state.list, modifier = modifier)
        } else {
            TaskDetailPane(state = detail, showBack = true, modifier = modifier)
        }
    }
}

private const val LIST_PANE_WEIGHT = 0.4f
