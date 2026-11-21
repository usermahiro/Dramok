# ThinksMovie

Aplikasi Android untuk menonton drama pendek dub Bahasa Indonesia.
Dev: **Mahiro**. Port dari `melolowrapper` (Python/Qt) ke Android: **inti Rust + UI Material 3 Expressive**.

## Arsitektur

```
rust/                         <- inti aplikasi (libthinksmovie_core.so)
  src/catalog.rs              sitemap melolo.com + API getSeriesBySlug; filter Indonesia via kolom `langs`
  src/proxy.rs                proxy 127.0.0.1 yang menambah Referer + meneruskan Range (port ProxyHandler)
  src/http.rs                 ureq + rustls (root CA dibundel)
  src/api.rs, src/lib.rs      JSON API + binding JNI
  tests/proxy.rs              uji integrasi proxy (Referer/Range/token)
app/                          <- UI Kotlin + Jetpack Compose (tipis, tanpa logika jaringan)
  ui/Theme.kt                 MaterialExpressiveTheme + MotionScheme.expressive() + dynamic color
  ui/HomeScreen.kt            grid drama, pencarian, tombol Snap, placeholder MaterialShapes
  ui/PlayerScreen.kt          Media3 ExoPlayer, geser ganti episode/drama, "Masuk Drama", daftar episode
```

Alur: Compose -> `NativeBridge` (JNI) -> Rust ambil data & URL video segar -> URL dibungkus
proxy lokal -> ExoPlayer memutar dari `http://127.0.0.1:<port>/...`.

## Build

Butuh: JDK 17, Android SDK 37.0 (`platforms;android-37.0`), build-tools 36.0.0, NDK 27.2.12479018, Rust + target Android,
`cargo-ndk`, Gradle 9.3.1 (AGP 9.1.1).

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
export ANDROID_NDK_HOME=/path/ke/ndk
gradle wrapper --gradle-version 9.3.1    # sekali saja, membuat gradlew
./gradlew :app:assembleRelease           # Gradle otomatis menjalankan cargo ndk
```

Tes inti Rust saja: `cd rust && cargo test`.

## Navigasi

- **Snap** di beranda: ambil drama Indonesia acak, langsung putar episode pertama.
- Saat mode Snap, geser layar atau tombol panah mengambil drama acak lain.
- **Masuk Drama**: mengunci drama saat ini; geser berikutnya mengganti episode di drama itu.
- Filter Indonesia memakai kolom `langs` dari API Melolo (`id`), bukan kata `dub` di alamat.
Build otomatis: push tag `v*` atau jalankan workflow **Build ThinksMovie** (APK masuk ke Release).

## Catatan

- Versi pin: Compose BOM 2026.09.00 (Compose 1.12.1) + Material 3 **1.5.0** memerlukan compileSdk 37
  dan AGP 9.1+. Build memakai AGP 9.1.1, Gradle 9.3.1, serta Kotlin bawaan AGP yang dipin ke 2.2.21
  agar cocok dengan plugin Compose. `minSdk` tetap 26 dan `targetSdk` tetap 36.
- Nama field poster di respons `getSeriesBySlug` belum diverifikasi (lihat `COVER_KEYS` di
  `rust/src/catalog.rs`). Jika poster kosong, UI memakai placeholder; sesuaikan kuncinya.
- APK release ditandatangani dengan kunci debug supaya mudah dipasang. Ganti untuk distribusi.
- Aplikasi ini klien untuk konten publik `melolo.com`, bukan port library native aplikasi aslinya.
