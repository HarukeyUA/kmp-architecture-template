package org.example.project.core.component

import app.cash.molecule.SnapshotNotifier
import com.arkivanov.decompose.GenericComponentContext
import org.example.project.core.component.snackbar.SnackbarHandler

interface AppComponentContext : GenericComponentContext<AppComponentContext> {
    val snackbarHandler: SnackbarHandler
    val snapshotNotifier: SnapshotNotifier
        get() = SnapshotNotifier.External
}
