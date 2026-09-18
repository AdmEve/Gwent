pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
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
