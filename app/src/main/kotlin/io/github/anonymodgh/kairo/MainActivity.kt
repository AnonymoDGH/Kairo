package io.github.anonymodgh.kairo

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.anonymodgh.kairo.core.ui.KairoColors
import io.github.anonymodgh.kairo.core.ui.KairoTheme
import io.github.anonymodgh.kairo.feature.diagnostics.DiagnosticsScreen
import io.github.anonymodgh.kairo.feature.home.HomeScreen
import java.util.function.Consumer

private enum class Screen { Home, Diagnostics }

class MainActivity : ComponentActivity() {
    /** Falso si el sistema no permite desenfocar lo de detrás (ahorro de batería, GPU, ajustes). */
    private val blurEnabled = mutableStateOf(true)
    private val blurListener = Consumer<Boolean> { enabled -> applyBlur(enabled) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        setContent {
            KairoTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.Home) }
                BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }
                // Sin desenfoque del sistema, un fondo gris sólido mantiene la legibilidad.
                val backdrop = if (blurEnabled.value) Color.Transparent else KairoColors.FallbackBackground
                Box(Modifier.fillMaxSize().background(backdrop)) {
                    when (screen) {
                        Screen.Home -> HomeScreen(onOpenSettings = { screen = Screen.Diagnostics })
                        Screen.Diagnostics -> DiagnosticsScreen(onBack = { screen = Screen.Home })
                    }
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        windowManager.addCrossWindowBlurEnabledListener(blurListener)
    }

    override fun onDetachedFromWindow() {
        windowManager.removeCrossWindowBlurEnabledListener(blurListener)
        super.onDetachedFromWindow()
    }

    private fun applyBlur(enabled: Boolean) {
        blurEnabled.value = enabled
        window.attributes = window.attributes.apply {
            blurBehindRadius = if (enabled) resources.getDimensionPixelSize(R.dimen.kairo_blur_behind) else 0
            dimAmount = if (enabled) 0.2f else 0f
        }
    }
}
