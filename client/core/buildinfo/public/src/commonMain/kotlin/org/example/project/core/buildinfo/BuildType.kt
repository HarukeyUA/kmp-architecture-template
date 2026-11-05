package org.example.project.core.buildinfo

/**
 * Whether a developer is watching this process. Decides what may reach a log, never what the app
 * does. Independent of [Environment]: an Android `prodDebug` install is a [DEBUG] build on [PROD].
 */
enum class BuildType {
    /** Android debug type, the iOS Debug framework, a dev desktop launch. */
    DEBUG,
    RELEASE,
}
