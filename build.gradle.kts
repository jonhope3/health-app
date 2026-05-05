plugins {
    // AGP 9.x has built-in Kotlin for Android app/library modules.
    // Keep kotlin.android here only as a version source for non-Android modules (none currently).
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
}
