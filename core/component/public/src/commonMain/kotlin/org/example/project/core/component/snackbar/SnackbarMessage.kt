package org.example.project.core.component.snackbar

data class SnackbarMessage(
    val text: String,
    val duration: SnackbarDuration = SnackbarDuration.Short,
)

enum class SnackbarDuration {
    Short,
    Long,
    Indefinite,
}
