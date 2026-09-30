import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun buildConfigString(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.codepassion.doubletriangle.feature.onboarding"
    compileSdk = 37
    buildFeatures {
        compose = true
        buildConfig = true
    }
    defaultConfig {
        minSdk = 28
        // This is the web OAuth client identifier, not a secret. It must be the
        // audience accepted by the API. Set GOOGLE_SERVER_CLIENT_ID in
        // local.properties (and the matching API environment) for each build.
        buildConfigField(
            "String",
            "GOOGLE_SERVER_CLIENT_ID",
            buildConfigString(
                localProperties.getProperty(
                    "GOOGLE_SERVER_CLIENT_ID",
                    "737395571151-0765h8e0g0sujldft4fj6h47leu7851h.apps.googleusercontent.com",
                ),
            ),
        )
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":feature:workout"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("com.android.billingclient:billing-ktx:8.2.1")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
