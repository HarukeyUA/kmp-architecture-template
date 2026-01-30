package org.example.project.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value

/**
 * Component that manages tabbed navigation using Decompose's childPages. Suitable for bottom
 * navigation patterns where all tabs should be retained.
 */
interface TabbedComponent<C : Any, T : Any> : ComponentContext {
    /** The current pages state, including all page instances and selected index. */
    val pages: Value<ChildPages<C, T>>

    /** Select a page by its index. */
    fun selectPage(index: Int)
}
