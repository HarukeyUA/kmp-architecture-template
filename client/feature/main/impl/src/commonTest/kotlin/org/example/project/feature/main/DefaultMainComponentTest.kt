package org.example.project.feature.main

import androidx.compose.runtime.Composable
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.component.AppComponentContext
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.feature.settings.SettingsComponent
import org.example.project.feature.settings.SettingsScreen
import org.example.project.feature.tasks.TaskListComponent
import org.example.project.feature.tasks.TasksComponent
import org.example.project.feature.tasks.TasksScreen

class DefaultMainComponentTest {
    @Test
    fun `starts on the tasks tab`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)

        assertThat(component.activeTab()).isEqualTo(MainComponent.Tab.Tasks)
    }

    @Test
    fun `a tab click opens that tab`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()

        component.state.value.eventSink(MainComponent.Event.TabClicked(MainComponent.Tab.Settings))

        assertThat(component.activeTab()).isEqualTo(MainComponent.Tab.Settings)
    }

    @Test
    fun `going back to a tab keeps its component`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()
        val tasks = component.stack.value.active.instance

        component.state.value.eventSink(MainComponent.Event.TabClicked(MainComponent.Tab.Settings))
        component.state.value.eventSink(MainComponent.Event.TabClicked(MainComponent.Tab.Tasks))

        assertThat(component.activeTab()).isEqualTo(MainComponent.Tab.Tasks)
        assertThat(component.stack.value.active.instance).isEqualTo(tasks)
    }

    @Test
    fun `back returns to the previous tab`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()
        component.state.value.eventSink(MainComponent.Event.TabClicked(MainComponent.Tab.Settings))

        component.onBackClick()

        assertThat(component.activeTab()).isEqualTo(MainComponent.Tab.Tasks)
    }

    private fun MainComponent.activeTab(): MainComponent.Tab = stack.value.active.configuration

    private fun component(lifecycle: LifecycleRegistry): MainComponent =
        DefaultMainComponent(
                componentContext = testComponentContext(lifecycle),
                tasksComponentFactory = { context -> FakeTasksComponent(context) },
                settingsComponentFactory = { context -> FakeSettingsComponent(context) },
                tasksScreen =
                    object : TasksScreen {
                        @Composable override fun Content(component: TasksComponent) = Unit
                    },
                settingsScreen =
                    object : SettingsScreen {
                        @Composable override fun Content(component: SettingsComponent) = Unit
                    },
            )
            .apply { state }

    private class FakeTasksComponent(context: AppComponentContext) :
        TasksComponent, AppComponentContext by context {
        override val state: StateFlow<TasksComponent.State> =
            MutableStateFlow(TasksComponent.State(list = TaskListComponent.State.Loading))
    }

    private class FakeSettingsComponent(context: AppComponentContext) :
        SettingsComponent, AppComponentContext by context {
        override val state: StateFlow<SettingsComponent.State> =
            MutableStateFlow(SettingsComponent.State(eventSink = {}))
    }
}
