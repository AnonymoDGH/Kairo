plugins {
    alias(libs.plugins.kairo.application)
    alias(libs.plugins.kairo.compose)
}

android {
    namespace = "io.github.anonymodgh.kairo"

    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "io.github.anonymodgh.kairo"
        versionCode = 2
        versionName = "0.1.0-fase1"
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
    implementation(project(":feature:diagnostics"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
