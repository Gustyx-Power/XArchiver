plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.xms.xarchiver.core.archive"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    // Add Java compatibility configuration
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Apache Commons Compress for handling various archive formats
    implementation("org.apache.commons:commons-compress:1.26.1")

    // Apache Commons IO for file utilities
    implementation("commons-io:commons-io:2.15.1")

    // For RAR support (optional if you want to add later)
    // implementation("com.github.junrar:junrar:7.5.5")
}
