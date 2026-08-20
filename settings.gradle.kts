pluginManagement {
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

rootProject.name = "doubleTriangleAndroid"
include(":app")
include(":core:designsystem")
include(":core:model")
include(":feature:workout")
include(":feature:onboarding")
