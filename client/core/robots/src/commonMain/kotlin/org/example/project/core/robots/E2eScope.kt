package org.example.project.core.robots

/**
 * The Metro scope every robot and journey contributes itself to. Each E2E host builds one graph of
 * this scope per test around its own `SemanticsNodeInteractionsProvider` and [Wait].
 */
// A DI scope marker, never instantiated or implemented; an interface would invite both.
@Suppress("AbstractClassCanBeInterface") abstract class E2eScope private constructor()
