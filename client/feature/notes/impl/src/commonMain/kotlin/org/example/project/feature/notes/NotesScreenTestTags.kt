package org.example.project.feature.notes

/**
 * Semantics tags for the notes screen, consumed by `:client:feature:notes:robots`. Declared next to
 * the screen they mark so a UI change and its tag change land in the same module.
 */
object NotesScreenTestTags {
    const val INPUT = "notes_input"
    const val ADD = "notes_add"
}
