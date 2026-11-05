package org.example.project.feature.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.serialization.Serializable
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.core.component.present

@AssistedInject
class DefaultTasksComponent(
    @Assisted componentContext: AppComponentContext,
    listFactory: TaskListComponent.Factory,
    detailFactory: TaskDetailComponent.Factory,
    createFactory: CreateTaskComponent.Factory,
) : TasksComponent, MoleculeComponent<TasksComponent.State>(componentContext) {

    private val detailNavigation = SlotNavigation<DetailConfig>()
    private val createNavigation = SlotNavigation<CreateConfig>()

    private val detailSlot =
        childSlot(
            source = detailNavigation,
            serializer = DetailConfig.serializer(),
            key = "detail",
            handleBackButton = true,
        ) { config, context ->
            detailFactory.create(
                componentContext = context,
                taskId = config.taskId,
                onClose = { detailNavigation.dismiss() },
            )
        }

    private val createSlot =
        childSlot(
            source = createNavigation,
            serializer = CreateConfig.serializer(),
            key = "create",
            handleBackButton = true,
        ) { _, context ->
            createFactory.create(
                componentContext = context,
                onDismiss = { createNavigation.dismiss() },
            )
        }

    private val list =
        listFactory.create(
            componentContext = componentContext.childContext(key = "list"),
            onTaskClick = { id -> detailNavigation.activate(DetailConfig(id)) },
            onAddClick = { createNavigation.activate(CreateConfig) },
        )

    @Composable
    override fun produceState(): TasksComponent.State {
        val detail by detailSlot.subscribeAsState()
        val create by createSlot.subscribeAsState()
        val openDetail = detail.child

        return TasksComponent.State(
            list = list.present(openDetail?.configuration?.taskId),
            // Keyed so the slots of one task's child are not handed to the next one's.
            detail = openDetail?.let { key(it.configuration) { it.instance.present() } },
            create = create.child?.instance?.present(),
        )
    }

    @Serializable private data class DetailConfig(val taskId: String)

    @Serializable private data object CreateConfig

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : TasksComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultTasksComponent
    }
}
