package org.example.project.core.ui.error

import androidx.compose.runtime.Composable
import org.example.project.core.error.AppError

internal class StubErrorRenderer : ErrorRenderer<AppError> {
    override fun resolveResource(error: AppError): ErrorRenderer.ResourceResult? = null

    @Composable override fun render(error: AppError): String = "Stub error"

    override suspend fun renderAsString(error: AppError): String = "Stub error"
}
