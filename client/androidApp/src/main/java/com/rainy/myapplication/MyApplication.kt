package com.rainy.myapplication

import android.app.Application
import org.example.project.AndroidAppGraph
import org.example.project.core.buildinfo.Environment
import org.example.project.createAndroidAppGraph

class MyApplication : Application() {
    // The flavor picked at install time (`installDevDebug` vs `installProdDebug`) decides which
    // server this install talks to. DEV_SERVER_HOST is the build machine's LAN IP, baked into the
    // dev flavor's BuildConfig (empty in the prod flavor and on off-network builds).
    val appGraph: AndroidAppGraph by lazy {
        createAndroidAppGraph(
            applicationContext = this,
            environment = if (BuildConfig.FLAVOR == "dev") Environment.DEV else Environment.PROD,
            devServerHost = BuildConfig.DEV_SERVER_HOST.takeIf { it.isNotEmpty() },
        )
    }
}
