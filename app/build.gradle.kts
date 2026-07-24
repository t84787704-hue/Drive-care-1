import java.util.Base64
import java.security.KeyStore

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystoreFile = rootProject.file("debug.keystore")
val base64KeystoreFile = rootProject.file("debug.keystore.base64")

val embeddedKeystoreBase64 = "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"

fun isKeystoreValid(file: java.io.File): Boolean {
    if (!file.exists() || file.length() < 1000L) return false
    return try {
        val ks = KeyStore.getInstance("JKS")
        file.inputStream().use { stream -> ks.load(stream, "android".toCharArray()) }
        ks.containsAlias("androiddebugkey")
    } catch (_: Exception) {
        false
    }
}

if (!isKeystoreValid(keystoreFile)) {
    try {
        val rawBase64 = if (base64KeystoreFile.exists() && base64KeystoreFile.length() > 0L) {
            base64KeystoreFile.readText()
        } else {
            embeddedKeystoreBase64
        }
        val cleanBase64 = rawBase64.replace("\\s".toRegex(), "")
        val bytes = Base64.getDecoder().decode(cleanBase64)
        keystoreFile.writeBytes(bytes)
    } catch (e: Exception) {
        println("Error restoring keystore: ${e.message}")
    }
}

android {
    namespace = "com.drivecare.app"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = keystoreFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
        getByName("debug") {
            storeFile = keystoreFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    defaultConfig {
        applicationId = "com.drivecare.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val roomVersion = "2.6.1"

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Room Database
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
