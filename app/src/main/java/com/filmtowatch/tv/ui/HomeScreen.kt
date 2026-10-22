package com.filmtowatch.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.filmtowatch.tv.data.GENRES
import com.filmtowatch.tv.data.REGIONS
import com.filmtowatch.tv.data.SortMode

@Composable
fun HomeScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    var focusedGenre by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = vm.homeTab != HomeTab.Discover) { vm.homeTab = HomeTab.Discover }

    val backdrop = when (vm.homeTab) {
        HomeTab.Discover -> (focusedGenre ?: GENRES.firstOrNull { it in data.genres })?.let { vm.genreArt[it] }
            ?: vm.genreArt.values.firstOrNull()
        HomeTab.Watchlist -> data.watchlist.firstOrNull { it.id == vm.lastOpenedFromWatchlist }?.background
            ?: data.watchlist.firstOrNull()?.background
    }

    Box(Modifier.fillMaxSize()) {
        BlurredBackdrop(backdrop, dim = 0.6f)
        Column(Modifier.fillMaxSize()) {
            TopBar(vm, data.watchlist.size)
            when (vm.homeTab) {
                HomeTab.Discover -> DiscoverTab(vm, onGenreFocused = { focusedGenre = it })
                HomeTab.Watchlist -> WatchlistTab(vm)
            }
        }
    }
}

@Composable
private fun TopBar(vm: AppViewModel, likedCount: Int) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 22.dp)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Palette.Pink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Film to Watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier.align(Alignment.Center).glass(RoundedCornerShape(50), Color.White.copy(alpha = 0.08f)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HomeTab.entries.forEach { tab ->
                TabPill(
                    label = if (tab == HomeTab.Watchlist && likedCount > 0) "${tab.label}  $likedCount" else tab.label,
                    selected = vm.homeTab == tab,
                    onSelect = { vm.homeTab = tab },
                )
            }
        }
        PillButton(
            "Support",
            onClick = { vm.navigate(Screen.Support) },
            icon = Icons.Filled.Favorite,
            iconTint = Palette.Pink,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/** tvOS top-bar tab: moving focus onto it switches the tab, like on Apple TV. */
@Composable
private fun TabPill(label: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.onFocusChanged { if (it.isFocused) onSelect() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Palette.GlassStrong else Color.Transparent,
            contentColor = if (selected) Color.White else Palette.Secondary,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp))
    }
}

@Composable
private fun DiscoverTab(vm: AppViewModel, onGenreFocused: (String?) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    val selectedCount = data.genres.size

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("What are you in the mood for?", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (selectedCount == 0) "Pick one or more genres — or none to see everything."
                        else "$selectedCount ${if (selectedCount == 1) "genre" else "genres"} selected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Palette.Secondary,
                    )
                }
                PillButton("Start Swiping", onClick = vm::startSwiping, icon = Icons.Filled.PlayArrow, large = true)
            }
        }

        item {
            val error = vm.catalogError
            if (error != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(error, style = MaterialTheme.typography.bodyLarge, color = Palette.Red, modifier = Modifier.weight(1f))
                    PillButton("Retry", onClick = vm::loadGenreArt, icon = Icons.Filled.Refresh)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                GENRES.chunked(GENRE_COLUMNS).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        row.forEachIndexed { colIndex, genre ->
                            GenreTile(
                                genre = genre,
                                art = vm.genreArt[genre],
                                selected = genre in data.genres,
                                onClick = { vm.toggleGenre(genre) },
                                onFocused = { onGenreFocused(genre) },
                                modifier = Modifier
                                    .weight(1f)
                                    .then(if (rowIndex == 0 && colIndex == 0) Modifier.focusRequester(firstFocus) else Modifier),
                            )
                        }
                        repeat(GENRE_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        item {
            SectionHeader("Order")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SortMode.entries.forEach { sort ->
                    ChoicePill(sort.label, selected = data.sort == sort, onClick = { vm.setSort(sort) })
                }
            }
        }

        if (vm.hasStreamingData) {
            item {
                val region = data.region ?: vm.region
                SectionHeader("Streaming region · ${REGIONS.firstOrNull { it.code == region }?.name ?: region}")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    items(REGIONS, key = { it.code }) { r ->
                        ChoicePill(r.code, selected = r.code == region, onClick = { vm.setRegion(r.code) })
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton("Start Swiping", onClick = vm::startSwiping, icon = Icons.Filled.PlayArrow, large = true)
                if (data.seen.isNotEmpty()) {
                    PillButton("Show ${data.seen.size} swiped movies again", onClick = vm::resetHistory, icon = Icons.Filled.Refresh)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Made by @0xProbabillity · github.com/0xProbabillity/film-to-watch-tv",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Tertiary,
            )
            Text(
                "Movie data from Cinemeta · ratings from IMDb",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Tertiary,
            )
            if (vm.hasStreamingData) {
                // Attribution required by the TMDB API terms
                Text(
                    "This product uses the TMDB API but is not endorsed or certified by TMDB. Streaming availability by JustWatch.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Tertiary,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (vm.restoreHomeFocus) {
            vm.restoreHomeFocus = false
            runCatching { firstFocus.requestFocus() }
        }
    }
}

@Composable
private fun GenreTile(
    genre: String,
    art: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(16f / 9f)
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1C1C1E), focusedContainerColor = Color(0xFF1C1C1E)),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(3.dp, Color.White), shape = shape) else Border.None,
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(elevationColor = Color.Black, elevation = 18.dp)),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = if (selected) 0.15f else 0.35f),
                            1f to Color.Black.copy(alpha = if (selected) 0.65f else 0.85f),
                        )
                    ),
            )
            Text(
                genre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Option capsule (segmented-control style). */
@Composable
fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Palette.GlassStrong else Palette.Glass.copy(alpha = 0.06f),
            contentColor = if (selected) Color.White else Palette.Secondary,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) Color.White.copy(alpha = 0.5f) else Palette.GlassBorder), shape = RoundedCornerShape(50)),
            focusedBorder = Border.None,
        ),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 12.dp),
    )
}

private const val GENRE_COLUMNS = 5
