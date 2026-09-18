plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ustad.personalassistant"
    compileSdk = 36
    defaultConfig { applicationId = "com.ustad.personalassistant"; minSdk = 26; targetSdk = 33; versionCode = 3; versionName = "0.1.2" }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "GMAIL_CLIENT_ID", "\"${providers.gradleProperty("GMAIL_CLIENT_ID").orNull ?: ""}\"")
        }
        getByName("release") {
            val ciKeystore = System.getenv("CI_KEYSTORE_FILE")
            val ciStorePassword = System.getenv("CI_KEYSTORE_PASSWORD")
            val ciKeyAlias = System.getenv("CI_KEY_ALIAS")
            val ciKeyPassword = System.getenv("CI_KEY_PASSWORD")
            if (!ciKeystore.isNullOrBlank() && !ciStorePassword.isNullOrBlank() && !ciKeyAlias.isNullOrBlank() && !ciKeyPassword.isNullOrBlank()) {
                signingConfig = signingConfigs.create("ciRelease").apply {
                    storeFile = file(ciKeystore)
                    storePassword = ciStorePassword
                    keyAlias = ciKeyAlias
                    keyPassword = ciKeyPassword
                }
            }
            buildConfigField("String", "GMAIL_CLIENT_ID", "\"${providers.gradleProperty("GMAIL_CLIENT_ID").orNull ?: ""}\"")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.09.00")); implementation("androidx.activity:activity-compose:1.10.1"); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.ui:ui-tooling-preview"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.material:material-icons-extended"); implementation("androidx.navigation:navigation-compose:2.9.3"); implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2"); implementation("androidx.datastore:datastore-preferences:1.1.7"); implementation("androidx.core:core-ktx:1.16.0"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
