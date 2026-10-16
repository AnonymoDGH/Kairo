package io.github.anonymodgh.kairo

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.anonymodgh.kairo.core.ui.KairoTheme
import io.github.anonymodgh.kairo.feature.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Comprueba que la pantalla de inicio compone y dibuja sin fallar en API 31 y 33+. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenSmokeTest {
    @get:Rule val compose = createComposeRule()

    private fun rendersHome() {
        compose.setContent { KairoTheme { HomeScreen(versionName = "test") } }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Kairo").assertExists()
    }

    @Test @Config(sdk = [31])
    fun rendersOnApi31() = rendersHome()

    @Test @Config(sdk = [35])
    fun rendersOnApi35() = rendersHome()
}
