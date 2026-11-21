plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mahiro.thinksmovie"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.mahiro.thinksmovie"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Ditandatangani dengan kunci debug agar APK hasil CI langsung bisa dipasang.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    sourceSets["main"].jniLibs.srcDir("src/main/jniLibs")
}

kotlin { jvmToolchain(17) }

// Compile inti Rust -> app/src/main/jniLibs/<abi>/libthinksmovie_core.so
// Butuh: rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
//        cargo install cargo-ndk ; ANDROID_NDK_HOME terisi.
// Lewati dengan: gradle -PskipRust assembleDebug (jika .so sudah ada).
val cargoNdk by tasks.registering(Exec::class) {
    workingDir = file("../rust")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a", "-t", "armeabi-v7a", "-t", "x86_64",
        "-o", file("src/main/jniLibs").absolutePath,
        "build", "--release",
    )
}
if (!project.hasProperty("skipRust")) {
    tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(cargoNdk) }
}

dependencies {
    // BOM 2026.09.00 = Compose 1.12.1 (compileSdk 37, AGP 9.1+). Material3 dinaikkan manual ke 1.5.0
    // karena API Material 3 Expressive baru publik di 1.5.0 (di 1.4.0 berstatus internal).
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.5.0")
    // Icons.* (PlayArrow, Search, Close, AutoMirrored.ArrowBack/Forward) tidak lagi ikut transitif.
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.graphics:graphics-shapes:1.0.1")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")

    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")

    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
}
