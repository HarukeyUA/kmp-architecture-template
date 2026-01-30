package com.rainy.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import org.example.project.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appGraph = (applicationContext as MyApplication).appGraph
        val rootScreen = appGraph.rootScreen

        val root =
            retainedComponent { componentContext ->
                appGraph.rootComponentFactory.create(componentContext)
            }

        setContent {
            App(root, rootScreen)
        }
    }
}
