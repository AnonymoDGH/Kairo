pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        ivy("https://github.com/k2-fsa/sherpa-onnx/releases/download") {
            name = "sherpa-onnx"
            patternLayout { artifact("v[revision]/[artifact]-[revision].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.k2fsa.sherpa.onnx", "sherpa-onnx") }
        }
    }
}

rootProject.name = "kairo"

include(
    ":app",
    ":core:ui",
    ":core:audio",
    ":core:conversation",
    ":core:listen",
    ":core:brain",
    ":core:voice",
    ":core:models",
    ":feature:home",
    ":feature:voices",
    ":feature:settings",
    ":feature:diagnostics",
)
