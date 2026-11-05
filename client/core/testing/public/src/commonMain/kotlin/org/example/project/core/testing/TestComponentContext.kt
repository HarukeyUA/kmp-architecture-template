package org.example.project.core.testing

import app.cash.molecule.SnapshotNotifier
import com.arkivanov.essenty.lifecycle.Lifecycle
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.DefaultAppComponentContext

fun testComponentContext(lifecycle: Lifecycle): AppComponentContext =
    DefaultAppComponentContext(
        lifecycle = lifecycle,
        snapshotNotifier = SnapshotNotifier.WhileActive,
    )
