plugins {
    alias(libs.plugins.kairo.library)
}

android {
    namespace = "io.github.anonymodgh.kairo.core.listen"
}

dependencies {
    implementation(project(":core:voice"))
    implementation(libs.kotlinx.coroutines.android)
}
