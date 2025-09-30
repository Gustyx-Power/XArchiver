// Root build.gradle.kts
plugins {
    id("com.android.application") version "8.12.3" apply false
    id("com.android.library") version "8.12.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
