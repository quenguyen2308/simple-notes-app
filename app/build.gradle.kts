import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

android {
    namespace = "com.yourname.simplenotes"
    compileSdk = 34

    signingConfigs {
        create("release") {
            storeFile = file("../simple-notes-release.jks")
            storePassword = System.getenv("KEYSTORE_PASS") ?: "SimpleNotes2026!"
            keyAlias = "simple-notes"
            keyPassword = System.getenv("KEY_PASS") ?: "SimpleNotes2026!"
        }
    }

    defaultConfig {
        applicationId = "com.yourname.simplenotes"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Compose compiler version matching Kotlin 1.9.21
        kotlinCompilerExtensionVersion = "1.5.6"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            // Exclude conflicting Apache HTTP components brought in by google-api-client-android
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Compose BOM — aligns all Compose version
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    //implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.appcompat)
    implementation(libs.core.ktx)

    // Room (offline-first local database)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WorkManager (background sync scheduling)
    implementation(libs.work.runtime.ktx)

    // Google Sign-In + Drive API v3
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android) {
        // Avoid duplicate Apache HTTP client classes
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson)

    // Koin DI
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)

    // Biometric (fingerprint + device credential unlock)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")

    // PIN hashing (bcrypt)
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Secure SharedPreferences for PIN storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Rich text editor (Compose Rich Editor by Mohamed Rejeb)
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc09")

    // Coil (image loading for Compose)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Gson (explicit — used for ContentBlock JSON serialization)
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.json:json:20231013")  // org.json not on JVM classpath by default
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-core:5.3.1")  // 5.6.1 not in repo; 5.3.1 is compatible with mockito-kotlin 5.1.0

    // Instrumented tests (Room in-memory + biometric)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.biometric:biometric:1.1.0")
}