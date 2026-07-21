package org.example.project

import androidx.compose.runtime.Composable

interface RootScreen {
    @Composable fun Content(component: RootComponent)
}
