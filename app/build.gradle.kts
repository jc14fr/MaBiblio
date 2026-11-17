import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Lecture seule de version.properties : le build ne modifie jamais ce fichier
val proprietes = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val versionMajeure = proprietes.getProperty("versionMajeure").toInt()
val versionMineure = proprietes.getProperty("versionMineure").toInt()
val versionBuild = proprietes.getProperty("buildNumero").toInt()

android {
    namespace = "fr.exemple.biblio"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.jc14fr.MaBiblio"
        minSdk = 26
        targetSdk = 35
        versionCode = versionMajeure * 1_000_000 + versionMineure * 10_000 + versionBuild
        versionName = "$versionMajeure.$versionMineure.$versionBuild"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    ksp("androidx.room:room-compiler:2.6.1")
}