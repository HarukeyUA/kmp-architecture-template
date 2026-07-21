package org.example.project.core.secure.storage

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.anifantakis.lib.ksafe.KSafe

@ContributesTo(AppScope::class)
interface JvmSecureStorageGraph {
    /** Desktop KSafe — encrypted file in the default app data directory. */
    @Provides @SingleIn(AppScope::class) fun provideKSafe(): KSafe = KSafe()
}
