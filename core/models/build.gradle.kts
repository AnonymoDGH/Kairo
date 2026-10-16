plugins {
    alias(libs.plugins.kairo.library)
}

android {
    namespace = "io.github.anonymodgh.kairo.core.models"
}

dependencies {
    api(libs.okhttp)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.commons.compress)

    testImplementation(libs.okhttp.mockwebserver3)
}
