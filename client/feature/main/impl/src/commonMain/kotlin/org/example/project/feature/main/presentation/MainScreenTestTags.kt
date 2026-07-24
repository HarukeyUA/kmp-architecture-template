package org.example.project.feature.main.presentation

/**
 * Semantics tags for the main tab scaffold, consumed by `:client:feature:main:robots`. Declared
 * next to the screen they mark so a UI change and its tag change land in the same module.
 */
object MainScreenTestTags {
    const val HOME_TAB = "main_home_tab"
    const val NOTES_TAB = "main_notes_tab"
    const val PROFILE_TAB = "main_profile_tab"
}
