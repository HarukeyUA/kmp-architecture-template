package org.example.project.core.buildinfo

/**
 * Which backend the build talks to. Decided once per process at graph creation by the platform
 * entry point (Android flavor `BuildConfig`, iOS `DEV` compilation condition, desktop `app.env`
 * launcher property), then injected — never sniffed from bundle ids or Gradle properties at
 * runtime.
 */
enum class Environment {
    DEV,
    PROD,
}
