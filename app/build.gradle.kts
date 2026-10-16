plugins {
    alias(libs.plugins.kairo.application)
    alias(libs.plugins.kairo.compose)
}

android {
    namespace = "io.github.anonymodgh.kairo"

    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "io.github.anonymodgh.kairo"
        versionCode = 1
        versionName = "0.0.1-fase0"
    }

    signingConfigs {
        // CI firma con un keystore de depuración fijo (secreto) para poder actualizar sin desinstalar.
        val ciKeystore = System.getenv("KAIRO_DEBUG_KEYSTORE")?.let(::file)?.takeIf { it.exists() }
        if (ciKeystore != null) {
            getByName("debug") {
                storeFile = ciKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
