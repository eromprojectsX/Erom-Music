plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.erom.music"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.erom.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "12.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
}