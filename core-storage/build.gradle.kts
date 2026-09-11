plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.xms.xarchiver.core.storage"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
    }

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
