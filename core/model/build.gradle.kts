import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun buildConfigString(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

plugins {
    id("com.android.library")
}

android {
    namespace = "io.codepassion.doubletriangle.core.model"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
        // These values are intentionally configurable per machine.  An emulator
        // normally reaches the local backend through 10.0.2.2; a physical device
        // can use its LAN address instead.
        buildConfigField("String", "WILDFORCE_API_BASE_URL", buildConfigString(localProperties.getProperty("WILDFORCE_API_BASE_URL", "https://www.wildforce.app/api")))
        buildConfigField("String", "WILDFORCE_PUBLIC_MEDIA_BASE_URL", buildConfigString(localProperties.getProperty("WILDFORCE_PUBLIC_MEDIA_BASE_URL", "https://khbsrnhvonggicfmhcpg.supabase.co/storage/v1/object/public/wildfit")))
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies { testImplementation("junit:junit:4.13.2") }
