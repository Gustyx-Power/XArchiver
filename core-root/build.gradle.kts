plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.xms.xarchiver.core.root"
    compileSdk = 34
    defaultConfig { minSdk = 23 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // libsu by topjohnwu (root)
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:io:6.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
