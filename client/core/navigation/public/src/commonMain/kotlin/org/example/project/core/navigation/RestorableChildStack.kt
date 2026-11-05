package org.example.project.core.navigation

import com.arkivanov.decompose.GenericComponentContext
import com.arkivanov.decompose.router.children.NavigationSource
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.consumeRequired
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer

/**
 * Marker for stack configurations whose screens must never be recreated from saved state — e.g. a
 * full-screen progress step, or a screen whose required input lives only in volatile memory and is
 * gone after process death.
 *
 * Honored by [restorableChildStack].
 */
interface NotRestorable

/**
 * A [childStack] that skips restoration when the saved stack contains a [NotRestorable]
 * configuration: the whole saved stack is then discarded and [initialStack] is used, exactly as if
 * nothing had been saved. Otherwise behaves like `childStack` with a serializer.
 *
 * The reset is all-or-nothing rather than dropping just the [NotRestorable] entries: Decompose
 * pairs saved child states with configurations positionally, so restoring a filtered stack is
 * undefined behavior.
 */
fun <Ctx : GenericComponentContext<Ctx>, C : Any, T : Any> Ctx.restorableChildStack(
    source: NavigationSource<StackNavigation.Event<C>>,
    serializer: KSerializer<C>,
    initialStack: () -> List<C>,
    key: String = "DefaultChildStack",
    handleBackButton: Boolean = false,
    childFactory: (configuration: C, Ctx) -> T,
): Value<ChildStack<C, T>> =
    childStack(
        source = source,
        initialStack = initialStack,
        saveStack = { stack ->
            SerializableContainer(value = stack, strategy = ListSerializer(serializer))
        },
        restoreStack = { container ->
            container.consumeRequired(strategy = ListSerializer(serializer)).takeIf { stack ->
                stack.none { it is NotRestorable }
            }
        },
        key = key,
        handleBackButton = handleBackButton,
        childFactory = childFactory,
    )
