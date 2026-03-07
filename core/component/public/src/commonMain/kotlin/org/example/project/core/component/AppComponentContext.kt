package org.example.project.core.component

import com.arkivanov.decompose.GenericComponentContext
import org.example.project.core.component.snackbar.SnackbarHandler

interface AppComponentContext : GenericComponentContext<AppComponentContext> {
    val snackbarHandler: SnackbarHandler
}
