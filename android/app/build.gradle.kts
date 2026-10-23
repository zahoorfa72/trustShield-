import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) localPropertiesFile.inputStream().use { localProperties.load(it) }
val supabaseUrl = localProperties.getProperty("TRUSTSHIELD_SUPABASE_URL", "https://example.invalid")
val supabaseAnonKey = localProperties.getProperty("TRUSTSHIELD_SUPABASE_ANON_KEY", "not-configured")
android {
    namespace = "com.trustshield.app"
    compileSdk = 36
    defaultConfig { applicationId = "com.trustshield.app"; minSdk = 29; targetSdk = 35; versionCode = 1; versionName = "0.1.0"; buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\""); buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"") }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
