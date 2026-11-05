package org.example.project.core.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.core.ui.topbar.AppLargeFlexibleTopBar
import org.example.project.core.ui.topbar.rememberAppExitUntilCollapsedScrollBehavior
import org.example.project.core.ui.topbar.topAppBarNestedScroll

/**
 * Full-width [AppLargeFlexibleTopBar] over a single-column [LazyColumn] body centered and capped at
 * [AppListContentMaxWidth]. The top bar stays unconstrained so it reads as page chrome on wide
 * windows while the list keeps a comfortable reading width.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    backButtonModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(top = 8.dp, bottom = 16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = rememberAppExitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.topAppBarNestedScroll(scrollBehavior, lazyListState),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppLargeFlexibleTopBar(
                title = title,
                subtitle = subtitle,
                onBackClick = onBack,
                backButtonModifier = backButtonModifier,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier =
                    Modifier.widthIn(max = AppListContentMaxWidth).fillMaxSize().imePadding(),
                state = lazyListState,
                contentPadding = contentPadding,
                verticalArrangement = verticalArrangement,
            ) {
                content()
                item {
                    Spacer(
                        Modifier.windowInsetsBottomHeight(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                        ),
                    )
                }
            }
        }
    }
}

val AppListContentMaxWidth = 720.dp
