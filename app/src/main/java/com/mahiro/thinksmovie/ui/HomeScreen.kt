package com.mahiro.thinksmovie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mahiro.thinksmovie.DramaMeta
import com.mahiro.thinksmovie.MainViewModel
import com.mahiro.thinksmovie.displayTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(vm: MainViewModel, gridState: LazyGridState) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("ThinksMovie", fontWeight = FontWeight.Bold) },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(vm)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    vm.listLoading -> LoadingIndicator(Modifier.align(Alignment.Center).size(72.dp))
                    vm.listError != null -> Column(
                        Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(vm.listError.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = vm::search) { Text("Coba lagi") }
                    }
                    vm.slugs.isEmpty() -> Text(
                        "Tidak ada drama ditemukan",
                        Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "${vm.total} drama dub Indonesia",
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        itemsIndexed(vm.slugs, key = { _, s -> s }) { index, slug ->
                            LaunchedEffect(slug) { vm.loadMeta(slug) }
                            DramaCard(slug, vm.metas[slug]) { vm.openDrama(index) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(vm: MainViewModel) {
    val focus = LocalFocusManager.current
    TextField(
        value = vm.query,
        onValueChange = { vm.query = it },
        placeholder = { Text("Cari drama dub Indonesia") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (vm.query.isNotEmpty()) {
                IconButton(onClick = { vm.query = ""; vm.search() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Hapus")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); vm.search() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DramaCard(slug: String, meta: DramaMeta?, onClick: () -> Unit) {
    val title = remember(slug, meta?.name) { displayTitle(slug, meta?.name) }
    Card(onClick = onClick, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            if (meta?.coverUrl != null) {
                AsyncImage(
                    model = meta.coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CoverPlaceholder(title)
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xE6000000))
                )
            )
            if (meta != null && meta.episodeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                ) {
                    Text(
                        "${meta.episodeCount} eps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            )
        }
    }
}

/** Placeholder saat poster belum ada: inisial di dalam bentuk MaterialShapes (cookie 9 sisi). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CoverPlaceholder(title: String) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(96.dp).clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                title.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
