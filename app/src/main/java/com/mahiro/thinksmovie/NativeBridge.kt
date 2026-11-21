package com.mahiro.thinksmovie

/** Jembatan JNI ke inti Rust (`rust/` -> libthinksmovie_core.so). Semua hasil berupa string JSON. */
object NativeBridge {
    init {
        System.loadLibrary("thinksmovie_core")
    }

    @JvmStatic external fun nativeStartProxy(): Int
    @JvmStatic external fun nativeCatalog(keyword: String, limit: Int): String
    @JvmStatic external fun nativeSeries(slug: String): String
    @JvmStatic external fun nativeEpisodeUrl(slug: String, episode: Int): String
}
