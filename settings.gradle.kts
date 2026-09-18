pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    // Centralizing versions here (rather than in the root build.gradle.kts with apply
    // false) means resolution only happens when a project actually applies the plugin.
    // :core never applies the Android ones, so a sandbox without SDK/google() access
    // (like this one) can still configure and test :core standalone.
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.0.20"
        id("org.jetbrains.kotlin.android") version "2.0.20"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
        id("com.android.application") version "8.5.2"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "gwent"

include(":core")

// The Android module needs the Android SDK (Android Studio provides it) to configure;
// it's excluded from the build graph in environments that don't have one, such as this
// sandbox, so `:core` still builds and tests standalone.
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    file("local.properties").exists()
if (hasAndroidSdk) {
    include(":android")
}
