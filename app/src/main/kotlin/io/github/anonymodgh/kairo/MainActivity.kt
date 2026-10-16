package io.github.anonymodgh.kairo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.anonymodgh.kairo.core.ui.KairoTheme
import io.github.anonymodgh.kairo.feature.diagnostics.DiagnosticsScreen
import io.github.anonymodgh.kairo.feature.home.HomeScreen

private enum class Screen { Home, Diagnostics }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            KairoTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.Home) }
                BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }
                when (screen) {
                    Screen.Home -> HomeScreen(
                        versionName = BuildConfig.VERSION_NAME,
                        onOpenDiagnostics = { screen = Screen.Diagnostics },
                    )
                    Screen.Diagnostics -> DiagnosticsScreen(onBack = { screen = Screen.Home })
                }
            }
        }
    }
}
