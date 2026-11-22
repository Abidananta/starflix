plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.starflix.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.starflix.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        val key = System.getenv("TMDB_API_KEY") ?: (project.findProperty("TMDB_API_KEY") as String?) ?: ""
        buildConfigField("String", "TMDB_API_KEY", "\"$key\"")
        val cat = System.getenv("CATALOG_URL") ?: (project.findProperty("CATALOG_URL") as String?) ?: ""
        buildConfigField("String", "CATALOG_URL", "\"$cat\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(bom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
