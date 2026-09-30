// The Windows certificate store includes the enterprise root certificate used on
// this development machine. The bundled JBR trust store does not, which prevents
// Gradle from resolving Android dependencies over HTTPS. Keep other platforms on
// their normal Java trust store.
if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT")
}

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
