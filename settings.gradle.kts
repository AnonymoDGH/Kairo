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
