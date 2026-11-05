package org.example.project

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arkivanov.decompose.retainedComponent
import org.example.project.core.component.DefaultAppComponentContext
import org.example.project.core.ui.theme.resolveDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val appGraph = (applicationContext as TemplateApplication).appGraph
        val rootScreen = appGraph.rootScreen

        val root = retainedComponent { componentContext ->
            appGraph.rootComponentFactory.create(DefaultAppComponentContext(componentContext))
        }

        splashScreen.setKeepOnScreenCondition {
            root.stack.value.active.instance is RootComponent.Child.Splash
        }

        setContent {
            val rootState by root.state.collectAsState()
            val darkTheme = rootState.themeMode.resolveDarkTheme()

            // By default bar icons follow the system setting, which the app's theme can override.
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle =
                        SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle =
                        SystemBarStyle.auto(NAV_BAR_LIGHT_SCRIM, NAV_BAR_DARK_SCRIM) { darkTheme },
                )
            }

            App(root, rootScreen)
        }
    }
}

// AndroidX's defaults, which are private: API 26-28 cannot always tint the icons, so they need one.
private val NAV_BAR_LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val NAV_BAR_DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
