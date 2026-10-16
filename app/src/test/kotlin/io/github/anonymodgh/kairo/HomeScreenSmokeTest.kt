package io.github.anonymodgh.kairo

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.anonymodgh.kairo.core.ui.KairoTheme
import io.github.anonymodgh.kairo.feature.diagnostics.DiagnosticsScreen
import io.github.anonymodgh.kairo.feature.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Comprueba que Inicio y Diagnóstico componen y dibujan sin fallar (Inicio en API 31 y 35). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenSmokeTest {
    @get:Rule val compose = createComposeRule()

    private fun rendersHome() {
        compose.setContent { KairoTheme { HomeScreen(versionName = "test", onOpenDiagnostics = {}) } }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Kairo").assertExists()
    }

    @Test @Config(sdk = [31])
    fun rendersOnApi31() = rendersHome()

    @Test @Config(sdk = [35])
    fun rendersOnApi35() = rendersHome()

    @Test @Config(sdk = [35])
    fun rendersDiagnostics() {
        compose.setContent { KairoTheme { DiagnosticsScreen(onBack = {}) } }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Diagnóstico").assertExists()
        System.getProperty("shot.out")?.let { out ->
            val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
            File(out).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
