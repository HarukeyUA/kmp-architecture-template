package org.example.project.core.ui.topbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.client.core.ui.Res
import org.example.project.client.core.ui.app_top_bar_back
import org.example.project.client.core.ui.ic_arrow_back
import org.example.project.core.ui.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Shared small top app bar for in-app navigation, built on the M3 small `TopAppBar` so it composes
 * with `TopAppBarScrollBehavior`. Pass [title] = null on screens that render their own in-content
 * header; the bar then collapses to a back button and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    backContentDescription: String = stringResource(Res.string.app_top_bar_back),
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = backContentDescription,
                    )
                }
            }
        },
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

/**
 * M3 Expressive's two-row top app bar: title plus optional subtitle, collapsing to a small bar on
 * scroll. Pass a [scrollBehavior] from `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` and
 * wire its `nestedScrollConnection` into the page's Modifier, or the collapse never triggers.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLargeFlexibleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    backButtonModifier: Modifier = Modifier,
    backContentDescription: String = stringResource(Res.string.app_top_bar_back),
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    LargeFlexibleTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
            )
        },
        subtitle =
            subtitle?.let {
                {
                    Text(
                        text = it,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick, modifier = backButtonModifier) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = backContentDescription,
                    )
                }
            }
        },
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

@Preview
@Composable
private fun AppTopBarBackOnlyPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) { AppTopBar(onBackClick = {}) }
    }
}

@Preview
@Composable
private fun AppTopBarTitlePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppTopBar(title = "Details", onBackClick = {})
        }
    }
}

@Preview
@Composable
private fun AppTopBarTitleWithActionsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppTopBar(
                title = "Details",
                onBackClick = {},
                actions = { TextButton(onClick = {}) { Text(text = "Save") } },
            )
        }
    }
}

@Preview
@Composable
private fun AppLargeFlexibleTopBarPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppLargeFlexibleTopBar(
                title = "A title long enough to need the large bar",
                subtitle = "A subtitle under it",
                onBackClick = {},
            )
        }
    }
}
