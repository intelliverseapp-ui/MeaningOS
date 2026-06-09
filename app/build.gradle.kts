import com.google.protobuf.gradle.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("com.google.protobuf") version "0.9.4"
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

        buildConfigField(
            "String",
            "GOOGLE_CLOUD_STT_API_KEY",
            "\"${project.properties["GOOGLE_CLOUD_STT_API_KEY"]}\""
        )

        buildConfigField(
            "String",
            "GOOGLE_CLOUD_TTS_API_KEY",
            "\"${project.properties["GOOGLE_CLOUD_TTS_API_KEY"]}\""
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
        buildConfig = true
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

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

protobuf {
    protoc {
        path = "/opt/homebrew/bin/protoc"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // FULL JAVA, NOT LITE
                create("java")
            }
            task.plugins {
                create("grpc") { }
                create("grpckt") { }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-logging:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    implementation("com.github.mik3y:usb-serial-for-android:3.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling:1.5.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.3")

    implementation("io.grpc:grpc-okhttp:1.62.2") {
        exclude(group = "com.google.protobuf")
    }
    implementation("io.grpc:grpc-protobuf:1.62.2") {
        exclude(group = "com.google.protobuf")
    }
    implementation("io.grpc:grpc-stub:1.62.2") {
        exclude(group = "com.google.protobuf")
    }
    implementation("io.grpc:grpc-kotlin-stub:1.4.1") {
        exclude(group = "com.google.protobuf")
    }
    implementation("io.grpc:grpc-auth:1.62.2") {
        exclude(group = "com.google.protobuf")
    }

    implementation("com.google.protobuf:protobuf-java:4.35.0")

    implementation("com.google.cloud:google-cloud-speech:4.78.0")

    implementation("com.google.auth:google-auth-library-oauth2-http:1.16.0")
}
