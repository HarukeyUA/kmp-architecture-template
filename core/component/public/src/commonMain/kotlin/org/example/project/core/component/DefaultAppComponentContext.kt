package org.example.project.core.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ComponentContextFactory
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackHandler
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import org.example.project.core.component.snackbar.ChildSnackbarHandler
import org.example.project.core.component.snackbar.SnackbarDispatcher
import org.example.project.core.component.snackbar.SnackbarHandler

class DefaultAppComponentContext(
    override val lifecycle: Lifecycle,
    stateKeeper: StateKeeper? = null,
    instanceKeeper: InstanceKeeper? = null,
    backHandler: BackHandler? = null,
    override val snackbarHandler: SnackbarHandler = SnackbarDispatcher(),
) : AppComponentContext {

    override val stateKeeper: StateKeeper = stateKeeper ?: StateKeeperDispatcher()
    override val instanceKeeper: InstanceKeeper =
        instanceKeeper
            ?: InstanceKeeperDispatcher().also { dispatcher ->
                lifecycle.doOnDestroy(dispatcher::destroy)
            }
    override val backHandler: BackHandler = backHandler ?: BackDispatcher()

    override val componentContextFactory: ComponentContextFactory<AppComponentContext> =
        ComponentContextFactory {
            childLifecycle,
            childStateKeeper,
            childInstanceKeeper,
            childBackHandler ->
            DefaultAppComponentContext(
                lifecycle = childLifecycle,
                stateKeeper = childStateKeeper,
                instanceKeeper = childInstanceKeeper,
                backHandler = childBackHandler,
                snackbarHandler = ChildSnackbarHandler(parent = snackbarHandler),
            )
        }

    constructor(
        componentContext: ComponentContext
    ) : this(
        lifecycle = componentContext.lifecycle,
        stateKeeper = componentContext.stateKeeper,
        instanceKeeper = componentContext.instanceKeeper,
        backHandler = componentContext.backHandler,
    )
}
