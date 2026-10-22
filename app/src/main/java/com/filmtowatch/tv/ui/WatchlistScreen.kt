package com.filmtowatch.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.filmtowatch.tv.data.Film
import com.filmtowatch.tv.data.Images

@Composable
fun WatchlistTab(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val films = data.watchlist
    val focus = remember { FocusRequester() }
    val focusId = vm.lastOpenedFromWatchlist?.takeIf { id -> films.any { it.id == id } } ?: films.firstOrNull()?.id

    if (films.isEmpty()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = Palette.Secondary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Your watchlist is empty", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text("Swipe right on movies you'd like to watch.", style = MaterialTheme.typography.bodyLarge, color = Palette.Secondary)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        state = vm.watchlistGridState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 48.dp),
    ) {
        items(films, key = { it.id }) { film ->
            PosterTile(
                film,
                modifier = if (film.id == focusId) Modifier.focusRequester(focus) else Modifier,
                onClick = { vm.navigate(Screen.Details(film.id, fromSwipe = false)) },
                onFocused = { vm.lastOpenedFromWatchlist = film.id },
            )
        }
    }

    LaunchedEffect(Unit) {
        if (vm.restoreHomeFocus) {
            vm.restoreHomeFocus = false
            runCatching { focus.requestFocus() }
        }
    }
}

/** Poster that lifts on focus and reveals its title underneath, like the Apple TV app. */
@Composable
private fun PosterTile(film: Film, modifier: Modifier, onClick: () -> Unit, onFocused: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
            shape = ClickableSurfaceDefaults.shape(shape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1C1C1E), focusedContainerColor = Color(0xFF1C1C1E)),
            glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(elevationColor = Color.Black, elevation = 20.dp)),
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = Images.poster(film.poster),
                    contentDescription = film.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            film.title,
            style = MaterialTheme.typography.labelLarge,
            color = if (focused) Color.White else Color.Transparent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
    }
}
