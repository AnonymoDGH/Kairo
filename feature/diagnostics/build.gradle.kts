plugins {
    alias(libs.plugins.kairo.library)
    alias(libs.plugins.kairo.compose)
}

android {
    namespace = "io.github.anonymodgh.kairo.feature.diagnostics"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:voice"))
    implementation(project(":core:listen"))
    implementation(project(":core:models"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
}
