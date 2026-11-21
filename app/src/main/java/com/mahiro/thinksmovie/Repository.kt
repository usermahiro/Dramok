package com.mahiro.thinksmovie

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class DramaMeta(
    val name: String,
    val coverUrl: String?,
    val episodeCount: Int,
    val episodes: List<Int>,
    val indonesian: Boolean = true,
)

/** "dubbed-cinta-1" -> "Dubbed Cinta 1" bila nama asli belum tersedia. */
fun displayTitle(slug: String, name: String?): String =
    name?.takeIf { it.isNotBlank() }
        ?: slug.split('-').filter { it.isNotEmpty() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

object Repository {
    private fun parse(json: String): JSONObject {
        val o = JSONObject(json)
        if (o.has("error")) throw IOException(o.optString("error", "kesalahan tidak diketahui"))
        return o
    }

    private fun JSONArray.ints(): List<Int> = List(length()) { getInt(it) }
    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

    /** Slug drama Indonesia + total yang cocok. */
    suspend fun catalog(keyword: String, limit: Int = 300): Pair<List<String>, Int> =
        withContext(Dispatchers.IO) {
            val o = parse(NativeBridge.nativeCatalog(keyword, limit))
            o.getJSONArray("slugs").strings() to o.getInt("total")
        }

    suspend fun meta(slug: String): DramaMeta = withContext(Dispatchers.IO) {
        val o = parse(NativeBridge.nativeSeries(slug))
        val eps = o.optJSONArray("episodes")?.ints().orEmpty()
        DramaMeta(
            name = o.optString("name", ""),
            coverUrl = o.optString("cover_proxy", "").ifEmpty { null },
            episodeCount = o.optInt("episode_num", eps.size),
            episodes = eps,
            indonesian = o.optBoolean("indonesian", true),
        )
    }

    /** URL video segar (sudah lewat proxy Referer lokal) untuk satu episode. */
    suspend fun episodeUrl(slug: String, episode: Int): String = withContext(Dispatchers.IO) {
        parse(NativeBridge.nativeEpisodeUrl(slug, episode)).getString("url")
    }
}
