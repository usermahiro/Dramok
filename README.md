# ThinksMovie

Aplikasi Android untuk menonton drama pendek dub Bahasa Indonesia.
Dev: **Mahiro**. Port dari `melolowrapper` (Python/Qt) ke Android: **inti Rust + UI Material 3 Expressive**.

## Arsitektur

```
rust/                         <- inti aplikasi (libthinksmovie_core.so)
  src/catalog.rs              sitemap melolo.com + API getSeriesBySlug (port Catalog di melolo_client.py)
  src/proxy.rs                proxy 127.0.0.1 yang menambah Referer + meneruskan Range (port ProxyHandler)
  src/http.rs                 ureq + rustls (root CA dibundel)
  src/api.rs, src/lib.rs      JSON API + binding JNI
  tests/proxy.rs              uji integrasi proxy (Referer/Range/token)
app/                          <- UI Kotlin + Jetpack Compose (tipis, tanpa logika jaringan)
  ui/Theme.kt                 MaterialExpressiveTheme + MotionScheme.expressive() + dynamic color
  ui/HomeScreen.kt            grid drama, pencarian, placeholder bentuk MaterialShapes
  ui/PlayerScreen.kt          Media3 ExoPlayer, Next Eps / Next Drama, daftar episode
```

Alur: Compose -> `NativeBridge` (JNI) -> Rust ambil data & URL video segar -> URL dibungkus
proxy lokal -> ExoPlayer memutar dari `http://127.0.0.1:<port>/...`.

## Build

Butuh: JDK 17, Android SDK 36, NDK, Rust + target Android, `cargo-ndk`, Gradle 8.13+.

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
export ANDROID_NDK_HOME=/path/ke/ndk
gradle wrapper --gradle-version 8.13     # sekali saja, membuat gradlew
./gradlew :app:assembleRelease           # Gradle otomatis menjalankan cargo ndk
```

Tes inti Rust saja: `cd rust && cargo test`.
Build otomatis: push tag `v*` atau jalankan workflow **Build ThinksMovie** (APK masuk ke Release).

## Catatan

- Versi pin: Compose BOM 2025.10.01 (Material 3 1.4.0, API Expressive masih `@Experimental`).
  Untuk BOM 2026.11.01 / Material 3 1.5.0 perlu Kotlin 2.2+ dan compileSdk 37.1.
- Nama field poster di respons `getSeriesBySlug` belum diverifikasi (lihat `COVER_KEYS` di
  `rust/src/catalog.rs`). Jika poster kosong, UI memakai placeholder; sesuaikan kuncinya.
- APK release ditandatangani dengan kunci debug supaya mudah dipasang. Ganti untuk distribusi.
- Aplikasi ini klien untuk konten publik `melolo.com`, bukan port library native aplikasi aslinya.
