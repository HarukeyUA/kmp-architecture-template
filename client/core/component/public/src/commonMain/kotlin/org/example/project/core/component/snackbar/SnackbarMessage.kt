package org.example.project.core.component.snackbar

import org.jetbrains.compose.resources.StringResource

class SnackbarMessage(
    val text: StringResource,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val action: SnackbarAction? = null,
)

class SnackbarAction(val label: StringResource, val onClick: () -> Unit)

enum class SnackbarDuration {
    Short,
    Long,
    Indefinite,
}
