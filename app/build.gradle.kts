plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.ecodim.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ecodim.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2.0"
    }

    // Release signing is optional: pass -PECODIMMER_STORE_FILE=... (and friends)
    // or set them in ~/.gradle/gradle.properties. Without them, release stays unsigned.
    val storeFilePath = findProperty("ECODIMMER_STORE_FILE") as String?
    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = findProperty("ECODIMMER_STORE_PASSWORD") as String?
                keyAlias = findProperty("ECODIMMER_KEY_ALIAS") as String?
                keyPassword = findProperty("ECODIMMER_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
}
