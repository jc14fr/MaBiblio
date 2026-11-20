
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}



android {
    namespace = "fr.exemple.biblio"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.exemple.biblio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1030003
        versionName = "1.3.0"
    }

    buildFeatures {
    compose = true
    buildConfig = true
}

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
    implementation("androidx.compose.material:material-icons-core")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    ksp("androidx.room:room-compiler:2.6.1")

// Copie l'APK sous un nom lisible après assembleDebug
tasks.matching { it.name == "assembleDebug" }.configureEach {
    doLast {
        val dossier = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        File(dossier, "app-debug.apk").takeIf { it.exists() }
            ?.copyTo(File(dossier, "MaBiblio-1.3.0.apk"), overwrite = true)
    }
}
}