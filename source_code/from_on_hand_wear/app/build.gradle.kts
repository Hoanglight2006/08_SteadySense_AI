plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace   = "vn.edu.uit.tpkd.wear"
    compileSdk  = 35

    defaultConfig {
        applicationId   = "vn.edu.uit.tpkd.wear"
        minSdk          = 30
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    // Wear OS
    implementation("androidx.wear:wear:1.3.0")

    // TFLite — FP32 (no GPU/NNAPI delegate; ARM CPU only per ICTA benchmark)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    // Core
    implementation("androidx.core:core-ktx:1.12.0")

    testImplementation("junit:junit:4.13.2")
}

