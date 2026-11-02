import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.multiplatform") version "2.4.20"
        id("org.jetbrains.kotlin.jvm") version "2.4.20"
        id("org.jetbrains.compose") version "1.12.2"
        id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
    }

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

rootProject.name = "VexLife-Prod"

include(":reference:compose")

// [VXG RealForever]
