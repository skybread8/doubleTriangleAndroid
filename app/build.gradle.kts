import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun buildConfigString(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Keep local builds usable until a Firebase project is configured. When the
// configuration is supplied, compile the real FCM service and its SDK.
val firebaseConfigured = file("google-services.json").isFile
if (firebaseConfigured) apply(plugin = "com.google.gms.google-services")

android {
    // The namespace is used by Android Studio's developer-verification flow;
    // it must match the Play package registered for this application.
    namespace = "io.codepassion.wildforce.android"
    sourceSets.getByName("main").kotlin.srcDir(if (firebaseConfigured) "src/fcm/java" else "src/noFcm/java")
    compileSdk = 37

    defaultConfig {
        applicationId = "io.codepassion.wildforce.android"
        minSdk = 28
        targetSdk = 37
        versionCode = 28
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "OPENAI_API_KEY", buildConfigString(localProperties.getProperty("OPENAI_API_KEY", "")))
        buildConfigField("String", "OPENAI_MODEL", buildConfigString(localProperties.getProperty("OPENAI_MODEL", "gpt-5.6-luna")))
        buildConfigField("boolean", "FCM_CONFIGURED", firebaseConfigured.toString())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        // Localization is reconciled in a separate workstream; keep code and
        // manifest lint actionable without failing on its intermediate state.
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    if (firebaseConfigured) {
        implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
        implementation("com.google.firebase:firebase-messaging")
    }
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":feature:workout"))
    implementation(project(":feature:onboarding"))
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.wear:wear-remote-interactions:1.2.0")
    implementation("com.google.android.gms:play-services-wearable:17.1.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    // Bundled Latin OCR mirrors iOS Vision and keeps label parsing available
    // when the user does not have AI nutrition access or the API is offline.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
