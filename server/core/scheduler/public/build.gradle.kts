plugins {
    alias(libs.plugins.convention.server.core.public)
}

// ScheduledJob is a plain contract (name + kotlin.time.Duration interval + suspend run()). The
// advisory-lock machinery that runs it lives entirely in :impl.
