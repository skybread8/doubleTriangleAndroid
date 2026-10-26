pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // This optional cache lets the offline development environment resolve
        // the bundled OCR dependency. Normal checkouts fall through to Google.
        maven { url = uri("$rootDir/.gradle-local-repo") }
        google()
        mavenCentral()
    }
}

rootProject.name = "doubleTriangleAndroid"
include(":app")
include(":core:designsystem")
include(":core:model")
include(":feature:workout")
include(":feature:onboarding")
include(":wear")
