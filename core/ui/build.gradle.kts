plugins {
    alias(libs.plugins.kairo.library)
    alias(libs.plugins.kairo.compose)
}

android {
    namespace = "io.github.anonymodgh.kairo.core.ui"
}

dependencies {
    api(libs.haze)
    api(libs.haze.glass)
}
