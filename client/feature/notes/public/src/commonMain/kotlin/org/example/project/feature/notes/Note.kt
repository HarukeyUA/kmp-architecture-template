package org.example.project.feature.notes

/**
 * The client-side notes model — what the UI renders. Deliberately *not* the wire `NoteResponse`:
 * the client shares the seam's wire types but maps them to its own model via `toModel()`, so the UI
 * is insulated from wire churn (ADR-0006).
 */
data class Note(val id: String, val text: String, val author: String)
