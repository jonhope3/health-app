plugins {
    id("com.android.application")
    // AGP 9.x bundles Kotlin for Android natively — do NOT add kotlin.android here.
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.fittrack.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fittrack.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.fittrack.app.test.HiltTestRunner"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = "android"
            keyAlias = "release"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // ── Compose BOM (pins all compose-* versions consistently) ────────────────
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)

    // ── AndroidX Core ─────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.16.0")

    // ── Lifecycle — 2.9.x brings SavedStateHandle flows + lifecycle-runtime-compose
    val lifecycleVersion = "2.9.0"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    // Provides collectAsStateWithLifecycle()
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")

    implementation("androidx.activity:activity-compose:1.10.1")

    // Fix IDE Unresolved reference: kotlin.Unit
    implementation(kotlin("stdlib"))

    // ── Compose UI ────────────────────────────────────────────────────────────
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // Material3 Adaptive (WindowSizeClass, ListDetailPaneScaffold)
    implementation("androidx.compose.material3.adaptive:adaptive:1.1.0")
    implementation("androidx.compose.material3.adaptive:adaptive-layout:1.1.0")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.1.0")
    // Animation extras (shared element transitions)
    implementation("androidx.compose.animation:animation")

    // ── Navigation (type-safe routes via @Serializable) ──────────────────────
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // ── Networking ────────────────────────────────────────────────────────────
    implementation("com.squareup.okhttp3:okhttp:5.0.0")

    // ── Kotlin / Coroutines ───────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // ── Room 2.8.x — SQLite ORM with Flow support ────────────────────────────
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")


    // ── Hilt — dependency injection ───────────────────────────────────────────
    val hiltVersion = "2.59.2"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")
    val hiltNavVersion = "1.3.0"
    implementation("androidx.hilt:hilt-navigation-compose:$hiltNavVersion")

    // ── WorkManager — background tasks ────────────────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // ── Health Connect ────────────────────────────────────────────────────────
    implementation("androidx.health.connect:connect-client:1.1.0")

    // ── ML Kit GenAI Prompt (Gemini Nano) ─────────────────────────────────────
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")

    // ── Google Fonts for Compose ──────────────────────────────────────────────
    implementation("androidx.compose.ui:ui-text-google-fonts")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ── Instrumented UI Tests (emulator only, no Gemini Nano required) ─────────
    val composeVersion = "1.8.1"
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.59.2")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.59.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
