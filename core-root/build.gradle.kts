plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.xms.xarchiver.core.root"
    compileSdk = 34
    defaultConfig { minSdk = 33 }
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check"); jvmTarget = "17" }
}

dependencies {
    // libsu by topjohnwu (root)
    val libsuVersion = "6.0.0"
    implementation("com.github.topjohnwu.libsu:core:$libsuVersion")
    implementation("com.github.topjohnwu.libsu:service:$libsuVersion")
    implementation("com.github.topjohnwu.libsu:nio:$libsuVersion")
    implementation("com.github.topjohnwu.libsu:io:$libsuVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
