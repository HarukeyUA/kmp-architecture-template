package org.example.project.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.client.feature.main.impl.Res
import org.example.project.client.feature.main.impl.ic_check_circle
import org.example.project.client.feature.main.impl.ic_settings
import org.example.project.client.feature.main.impl.main_tab_settings
import org.example.project.client.feature.main.impl.main_tab_tasks
import org.example.project.core.ui.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Inject
@ContributesBinding(AppScope::class)
class DefaultMainScreen : MainScreen {
    @Composable
    override fun Content(component: MainComponent) {
        val state by component.state.collectAsStateWithLifecycle()
        val stack by component.stack.subscribeAsState()
        val isExpanded =
            currentWindowAdaptiveInfoV2()
                .windowSizeClass
                .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

        MainScreenContent(
            state = state,
            activeTab = stack.active.configuration,
            isExpanded = isExpanded,
        ) {
            Children(stack = component.stack, animation = stackAnimation(fade())) {
                it.instance.Content()
            }
        }
    }
}

@Composable
internal fun MainScreenContent(
    state: MainComponent.State,
    activeTab: MainComponent.Tab,
    isExpanded: Boolean,
    content: @Composable () -> Unit,
) {
    val onTabClick = { tab: MainComponent.Tab ->
        state.eventSink(MainComponent.Event.TabClicked(tab))
    }
    if (isExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(modifier = Modifier.testTag(MainScreenTestTags.RAIL)) {
                TabItem.entries.forEach { item ->
                    NavigationRailItem(
                        selected = activeTab == item.tab,
                        onClick = { onTabClick(item.tab) },
                        icon = { TabIcon(item) },
                        label = { Text(text = stringResource(item.label)) },
                        modifier = Modifier.testTag(item.testTag),
                    )
                }
            }
            Box(
                modifier =
                    Modifier.weight(1f)
                        .consumeWindowInsets(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Start),
                        ),
            ) {
                content()
            }
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag(MainScreenTestTags.BOTTOM_BAR)) {
                    TabItem.entries.forEach { item ->
                        NavigationBarItem(
                            selected = activeTab == item.tab,
                            onClick = { onTabClick(item.tab) },
                            icon = { TabIcon(item) },
                            label = { Text(text = stringResource(item.label)) },
                            modifier = Modifier.testTag(item.testTag),
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) { content() }
        }
    }
}

@Composable
private fun TabIcon(item: TabItem) {
    Icon(painter = painterResource(item.icon), contentDescription = null)
}

private enum class TabItem(
    val tab: MainComponent.Tab,
    val label: StringResource,
    val icon: DrawableResource,
    val testTag: String,
) {
    Tasks(
        tab = MainComponent.Tab.Tasks,
        label = Res.string.main_tab_tasks,
        icon = Res.drawable.ic_check_circle,
        testTag = MainScreenTestTags.TASKS_TAB,
    ),
    Settings(
        tab = MainComponent.Tab.Settings,
        label = Res.string.main_tab_settings,
        icon = Res.drawable.ic_settings,
        testTag = MainScreenTestTags.SETTINGS_TAB,
    ),
}

@Preview
@Composable
private fun MainScreenCompactPreview() {
    AppTheme {
        MainScreenContent(
            state = MainComponent.State(eventSink = {}),
            activeTab = MainComponent.Tab.Tasks,
            isExpanded = false,
        ) {}
    }
}

@Preview(widthDp = 900)
@Composable
private fun MainScreenExpandedPreview() {
    AppTheme {
        MainScreenContent(
            state = MainComponent.State(eventSink = {}),
            activeTab = MainComponent.Tab.Settings,
            isExpanded = true,
        ) {}
    }
}
