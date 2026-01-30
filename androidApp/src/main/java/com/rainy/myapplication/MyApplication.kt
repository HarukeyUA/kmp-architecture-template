package com.rainy.myapplication

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import org.example.project.AndroidAppGraph

class MyApplication: Application() {
    val appGraph by lazy { createGraphFactory<AndroidAppGraph.Factory>().create(this) }
}