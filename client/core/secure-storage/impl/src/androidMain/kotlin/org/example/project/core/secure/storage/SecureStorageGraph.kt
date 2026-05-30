package org.example.project.core.secure.storage

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.anifantakis.lib.ksafe.KSafe

@ContributesTo(AppScope::class)
interface AndroidSecureStorageGraph {
    /** Android Keystore-backed KSafe; [Context] comes from the app graph. */
    @Provides @SingleIn(AppScope::class) fun provideKSafe(context: Context): KSafe = KSafe(context)
}
