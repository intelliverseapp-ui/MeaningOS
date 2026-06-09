pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }

    plugins {
        id("com.android.application") version "8.1.4"
        id("com.android.library") version "8.1.4"
        id("org.jetbrains.kotlin.android") version "1.9.10"
        kotlin("jvm") version "1.9.10"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "MeaningOS"
include(":app")
