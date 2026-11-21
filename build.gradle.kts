buildscript {
    repositories { mavenCentral() }
    dependencies {
        // Samakan Kotlin bawaan AGP dengan versi plugin Compose.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    }
}

plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
