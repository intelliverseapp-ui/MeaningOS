plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

android {
    namespace = "com.example.meaningosapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.meaningosapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // ⭐ Expose your Google Cloud STT key to BuildConfig
        buildConfigField(
            "String",
            "GOOGLE_CLOUD_STT_API_KEY",
            "\"${project.properties["GOOGLE_CLOUD_STT_API_KEY"]}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true   // ⭐ REQUIRED FIX — enables BuildConfig fields
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
    implementation("androidx.compose.material3:material3:1.1.2")

    // ⭐ FIX: Compose ViewModel integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Ktor 2.x Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-logging:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // USB Serial library
    implementation("com.github.mik3y:usb-serial-for-android:3.6.0")

    // ⭐ ML Kit removed — this was the failing dependency
    // implementation("com.google.mlkit:speech-recognition:16.0.0-beta3")

    // -----------------------------
    // ⭐ Google Cloud Speech gRPC
    // -----------------------------
    implementation("com.google.cloud:google-cloud-speech:4.6.0")

    // gRPC core
    implementation("io.grpc:grpc-okhttp:1.63.0")
    implementation("io.grpc:grpc-protobuf-lite:1.63.0")
    implementation("io.grpc:grpc-stub:1.63.0")

    // Kotlin coroutines support for gRPC
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling:1.5.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.3")
}
// Force protobuf-javalite and exclude protobuf-java
configurations.all {
    resolutionStrategy.force("com.google.protobuf:protobuf-javalite:3.25.1")
    exclude(group = "com.google.protobuf", module = "protobuf-java")
}
