package org.example.project.feature.settings

import org.example.project.core.local.storage.model.ThemeMode

/** Semantics tags for the settings screen, consumed by `:client:feature:settings:robots`. */
object SettingsScreenTestTags {
    const val LOG_OUT = "settings_log_out"

    fun theme(mode: ThemeMode): String = "settings_theme_${mode.name}"
}
