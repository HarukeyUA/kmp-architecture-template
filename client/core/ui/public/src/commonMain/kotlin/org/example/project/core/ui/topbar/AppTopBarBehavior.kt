package org.example.project.core.ui.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

/**
 * App-wide replacement for `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`. Touch platforms
 * snap on release; desktop does not, because mouse-wheel scrolling has no release event to snap on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun rememberAppExitUntilCollapsedScrollBehavior(): TopAppBarScrollBehavior
