plugins {
    alias(libs.plugins.kairo.library)
    alias(libs.plugins.kairo.compose)
}

android {
    namespace = "io.github.anonymodgh.kairo.feature.home"
}

dependencies {
    implementation(project(":core:ui"))
}
