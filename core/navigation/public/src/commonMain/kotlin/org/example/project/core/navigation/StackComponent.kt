package org.example.project.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

/**
 * Component that manages a stack of child components.
 *
 * Use this for:
 * - Flow coordinators (onboarding, checkout, wizards)
 * - Nested navigation (feature-internal routing)
 *
 * The stack is managed via Decompose's childStack. Use the ChildStack composable from
 * :core:ui:public to render the active child with animations.
 *
 * @param C Configuration type for child components
 * @param T Child component type
 */
interface StackComponent<out C : Any, out T : Any> : ComponentContext {
    /** The current stack state with the active child on top. */
    val stack: Value<ChildStack<C, T>>

    fun onBackClick()
}
