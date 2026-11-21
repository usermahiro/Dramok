package com.mahiro.thinksmovie

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class MainViewModel : ViewModel() {
    // ---- Katalog ----
    var query by mutableStateOf("")
    var slugs by mutableStateOf<List<String>>(emptyList()); private set
    var total by mutableIntStateOf(0); private set
    var listLoading by mutableStateOf(true); private set
    var listError by mutableStateOf<String?>(null); private set
    val metas = mutableStateMapOf<String, DramaMeta>()

    private val inFlight = HashSet<String>()
    private val gate = Semaphore(4)

    // ---- Pemutar ----
    var dramaIndex by mutableIntStateOf(-1); private set
    var episodes by mutableStateOf<List<Int>>(emptyList()); private set
    var episodeIndex by mutableIntStateOf(-1); private set
    var playUrl by mutableStateOf<String?>(null); private set
    var playError by mutableStateOf<String?>(null); private set
    private var playToken = 0

    val currentSlug: String? get() = slugs.getOrNull(dramaIndex)
    val currentTitle: String
        get() = currentSlug?.let { displayTitle(it, metas[it]?.name) }.orEmpty()
    val hasPrevEpisode: Boolean get() = episodeIndex > 0
    val hasNextEpisode: Boolean get() = episodeIndex in 0 until episodes.size - 1
    val hasNextDrama: Boolean get() = dramaIndex in 0 until slugs.size - 1

    init {
        search()
    }

    fun search() {
        viewModelScope.launch {
            listLoading = true
            listError = null
            try {
                val (s, t) = Repository.catalog(query.trim())
                slugs = s
                total = t
            } catch (e: Exception) {
                listError = e.message ?: "Gagal memuat katalog"
            } finally {
                listLoading = false
            }
        }
    }

    /** Muat poster/nama/jumlah episode untuk kartu yang sedang terlihat. */
    fun loadMeta(slug: String) {
        if (metas.containsKey(slug) || !inFlight.add(slug)) return
        viewModelScope.launch {
            try {
                gate.withPermit { metas[slug] = Repository.meta(slug) }
            } catch (_: Exception) {
                // Kartu tetap tampil dengan placeholder.
            } finally {
                inFlight.remove(slug)
            }
        }
    }

    fun openDrama(index: Int) {
        val slug = slugs.getOrNull(index) ?: return
        val token = ++playToken
        dramaIndex = index
        episodes = emptyList()
        episodeIndex = -1
        playUrl = null
        playError = null
        viewModelScope.launch {
            try {
                val meta = metas[slug] ?: Repository.meta(slug).also { metas[slug] = it }
                if (token != playToken) return@launch
                episodes = meta.episodes
                if (meta.episodes.isEmpty()) playError = "Drama ini belum punya episode." else playEpisode(0)
            } catch (e: Exception) {
                if (token == playToken) playError = e.message ?: "Gagal memuat drama"
            }
        }
    }

    fun playEpisode(index: Int) {
        val slug = currentSlug ?: return
        val ep = episodes.getOrNull(index) ?: return
        val token = ++playToken
        episodeIndex = index
        playUrl = null
        playError = null
        viewModelScope.launch {
            try {
                val url = Repository.episodeUrl(slug, ep)
                if (token == playToken) playUrl = url
            } catch (e: Exception) {
                if (token == playToken) playError = e.message ?: "Gagal mengambil video"
            }
        }
    }

    fun nextEpisode() { if (hasNextEpisode) playEpisode(episodeIndex + 1) }
    fun prevEpisode() { if (hasPrevEpisode) playEpisode(episodeIndex - 1) }
    fun nextDrama() { if (hasNextDrama) openDrama(dramaIndex + 1) }

    fun retry() {
        if (episodes.isEmpty()) openDrama(dramaIndex) else playEpisode(episodeIndex)
    }

    fun reportPlayerError(message: String) { playError = message }

    fun closePlayer() {
        playToken++
        dramaIndex = -1
        episodeIndex = -1
        episodes = emptyList()
        playUrl = null
        playError = null
    }
}
