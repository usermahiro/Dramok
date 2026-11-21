package com.mahiro.thinksmovie.ui

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mahiro.thinksmovie.MainViewModel
import kotlinx.coroutines.delay

private val PauseIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); close()
        moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); close()
    }.build()
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, UnstableApi::class)
@Composable
fun PlayerScreen(vm: MainViewModel) {
    BackHandler { vm.closePlayer() }

    val context = LocalContext.current
    val exo = remember { ExoPlayer.Builder(context).build().apply { playWhenReady = true } }
    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seekFraction by remember { mutableStateOf<Float?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showEpisodes by remember { mutableStateOf(false) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                ended = state == Player.STATE_ENDED
            }
            override fun onPlayerError(error: PlaybackException) {
                vm.reportPlayerError("Gagal memutar video (${error.errorCodeName})")
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener); exo.release() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exo) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) exo.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(vm.playUrl) {
        val url = vm.playUrl
        ended = false
        if (url == null) {
            exo.stop(); exo.clearMediaItems()
        } else {
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.play()
        }
    }

    LaunchedEffect(exo) {
        while (true) {
            position = exo.currentPosition
            exo.duration.takeIf { it > 0 }?.let { duration = it }
            delay(250)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(AndroidColor.BLACK)
                    player = exo
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Ketuk video untuk menampilkan/menyembunyikan kontrol.
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsVisible = !controlsVisible }
        )

        if (vm.playError == null && (vm.playUrl == null || buffering)) {
            LoadingIndicator(Modifier.align(Alignment.Center).size(72.dp))
        }

        vm.playError?.let { msg ->
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(msg, color = Color.White)
                FilledTonalButton(onClick = vm::retry) { Text("Coba lagi") }
            }
        }

        if (ended && vm.playError == null) {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Episode selesai",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = Color.White,
                )
                if (vm.hasNextEpisode) {
                    ExtendedFloatingActionButton(onClick = vm::nextEpisode) { Text("Buka Episode Berikutnya") }
                } else if (vm.hasNextDrama) {
                    ExtendedFloatingActionButton(onClick = vm::nextDrama) { Text("Drama Berikutnya") }
                }
            }
        }

        // ---- Bilah atas ----
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Row(
                Modifier.statusBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = vm::closePlayer) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Spacer(Modifier.width(12.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0x99000000)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            vm.currentTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        if (vm.episodeIndex >= 0) {
                            Text(
                                "Episode ${vm.episodes.getOrNull(vm.episodeIndex) ?: "-"} dari ${vm.episodes.size}",
                                color = Color(0xCCFFFFFF),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }

        // ---- Kontrol bawah ----
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Slider(
                        value = (seekFraction ?: (position.toFloat() / duration.coerceAtLeast(1))).coerceIn(0f, 1f),
                        onValueChange = { seekFraction = it },
                        onValueChangeFinished = {
                            seekFraction?.let { exo.seekTo((it * duration).toLong()) }
                            seekFraction = null
                        },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmt(position), style = MaterialTheme.typography.labelMedium)
                        Text(fmt(duration), style = MaterialTheme.typography.labelMedium)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconButton(onClick = vm::prevEpisode, enabled = vm.hasPrevEpisode) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Episode sebelumnya")
                        }
                        // Bentuk tombol berubah (lingkaran <-> cookie) mengikuti status putar.
                        FilledIconButton(
                            onClick = { if (isPlaying) exo.pause() else exo.play() },
                            shape = if (isPlaying) CircleShape else MaterialShapes.Cookie9Sided.toShape(),
                            modifier = Modifier.size(64.dp),
                        ) {
                            Icon(
                                if (isPlaying) PauseIcon else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        FilledTonalButton(onClick = vm::nextEpisode, enabled = vm.hasNextEpisode) {
                            Text("Next Eps")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(18.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showEpisodes = true },
                            enabled = vm.episodes.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) { Text("Daftar Episode") }
                        Button(
                            onClick = vm::nextDrama,
                            enabled = vm.hasNextDrama,
                            modifier = Modifier.weight(1f),
                        ) { Text("Next Drama") }
                    }
                }
            }
        }
    }

    if (showEpisodes) {
        ModalBottomSheet(onDismissRequest = { showEpisodes = false }) {
            Text(
                "Pilih episode",
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(72.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 420.dp).navigationBarsPadding(),
            ) {
                itemsIndexed(vm.episodes) { index, ep ->
                    val pick = { vm.playEpisode(index); showEpisodes = false }
                    if (index == vm.episodeIndex) {
                        Button(onClick = pick, contentPadding = PaddingValues(0.dp)) { Text("$ep") }
                    } else {
                        FilledTonalButton(onClick = pick, contentPadding = PaddingValues(0.dp)) { Text("$ep") }
                    }
                }
            }
        }
    }
}
