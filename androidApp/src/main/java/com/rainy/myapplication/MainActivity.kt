package com.rainy.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arkivanov.decompose.retainedComponent
import com.arkivanov.decompose.router.stack.active
import org.example.project.App
import org.example.project.RootComponent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val appGraph = (applicationContext as MyApplication).appGraph
        val rootScreen = appGraph.rootScreen

        val root = retainedComponent { componentContext ->
            appGraph.rootComponentFactory.create(componentContext)
        }

        splashScreen.setKeepOnScreenCondition {
            root.stack.value.active.instance is RootComponent.Child.Splash
        }

        setContent { App(root, rootScreen) }
    }
}
