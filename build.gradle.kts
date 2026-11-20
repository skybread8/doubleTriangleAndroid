buildscript {
    repositories { google(); mavenCentral() }
    dependencies {
        if (file("app/google-services.json").isFile) {
            classpath("com.google.gms:google-services:4.6.0")
        }
    }
}

plugins {
    id("com.android.application") version "9.3.3" apply false
    id("com.android.library") version "9.3.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
