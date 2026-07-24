plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystorePath = System.getenv("KEYSTORE_PATH")
    ?: System.getenv("KEYSTORE_FILE")
    ?: System.getenv("RELEASE_KEYSTORE_PATH")
val storePasswordEnv = System.getenv("KEYSTORE_PASSWORD")
    ?: System.getenv("STORE_PASSWORD")
    ?: System.getenv("RELEASE_STORE_PASSWORD")
val keyAliasEnv = System.getenv("KEY_ALIAS")
    ?: System.getenv("RELEASE_KEY_ALIAS")
val keyPasswordEnv = System.getenv("KEY_PASSWORD")
    ?: System.getenv("RELEASE_KEY_PASSWORD")

val envKeystore = keystorePath?.let { file(it) }
val rootDebugKeystore = rootProject.file("debug.keystore")
val localDebugKeystore = file("debug.keystore")

val resolvedKeystoreFile = when {
    envKeystore != null && envKeystore.exists() -> envKeystore
    rootDebugKeystore.exists() -> rootDebugKeystore
    localDebugKeystore.exists() -> localDebugKeystore
    else -> null
}

android {
    namespace = "com.drivecare.app"
    compileSdk = 35

    signingConfigs {
        if (resolvedKeystoreFile != null) {
            create("release") {
                storeFile = resolvedKeystoreFile
                storePassword = storePasswordEnv ?: "android"
                keyAlias = keyAliasEnv ?: "androiddebugkey"
                keyPassword = keyPasswordEnv ?: "android"
                enableV1Signing = true
                enableV2Signing = true
            }
            getByName("debug") {
                storeFile = resolvedKeystoreFile
                storePassword = storePasswordEnv ?: "android"
                keyAlias = keyAliasEnv ?: "androiddebugkey"
                keyPassword = keyPasswordEnv ?: "android"
                enableV1Signing = true
                enableV2Signing = true
            }
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
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null && releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            val debugSigning = signingConfigs.getByName("debug")
            if (debugSigning.storeFile?.exists() == true) {
                signingConfig = debugSigning
            }
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
