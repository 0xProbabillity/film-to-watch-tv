package com.filmtowatch.tv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.filmtowatch.tv.data.Film
import com.filmtowatch.tv.data.Images
import com.filmtowatch.tv.data.SortMode
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val data by vm.data.collectAsStateWithLifecycle()
    val top = vm.deck.firstOrNull()
    val next = vm.deck.getOrNull(1)
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val offsetX = remember { Animatable(0f) }
    val flyDistance = with(LocalDensity.current) { 900.dp.toPx() }
    var animating by remember { mutableStateOf(false) }
    val progress = { offsetX.value / flyDistance }

    fun fling(like: Boolean) {
        if (animating || top == null) return
        animating = true
        scope.launch {
            offsetX.animateTo(if (like) flyDistance else -flyDistance, tween(320, easing = FastOutLinearInEasing))
            vm.swipe(like)
            offsetX.snapTo(0f)
            animating = false
        }
    }

    // Warm up the image cache for the next few cards so swiping feels instant.
    val upcoming = vm.deck.take(4).map { it.id }
    LaunchedEffect(upcoming) {
        vm.deck.take(4).forEach { film ->
            listOfNotNull(Images.poster(film.poster)).forEach {
                context.imageLoader.enqueue(ImageRequest.Builder(context).data(it).build())
            }
            film.background?.let {
                context.imageLoader.enqueue(ImageRequest.Builder(context).data(it).size(64, 36).build())
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .onPreviewKeyEvent { event ->
                if (top == null || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> fling(like = true)
                    Key.DirectionLeft -> fling(like = false)
                    Key.DirectionDown, Key.DirectionCenter, Key.Enter, Key.NumPadEnter ->
                        if (!animating) vm.navigate(Screen.Details(top.id, fromSwipe = true))
                    Key.DirectionUp -> if (!animating && !vm.undo()) Launchers.toast(context, "Nothing to undo")
                    Key.MediaPlayPause, Key.MediaPlay -> Launchers.playTrailer(context, top.trailerId)
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .focusable(),
    ) {
        BlurredBackdrop(top?.background, dim = 0.45f)

        // Top bar
        Row(
            Modifier.align(Alignment.TopStart).padding(horizontal = 48.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Palette.Pink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Film to Watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp))
            val filters = listOf(data.sort.label) + data.genres.sorted()
            Text(filters.joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium, color = Palette.Secondary, maxLines = 1)
        }
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 48.dp, vertical = 20.dp)
                .glass(RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Palette.Pink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("${data.watchlist.size}", style = MaterialTheme.typography.titleSmall)
        }

        if (top == null) {
            EmptyDeck(vm, data.sort, Modifier.align(Alignment.Center))
        } else {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (-14).dp)
                    .fillMaxHeight(0.76f)
                    .aspectRatio(2f / 3f),
            ) {
                if (next != null) {
                    key(next.id) {
                        FilmCard(
                            film = next,
                            vm = vm,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                val p = abs(progress()).coerceIn(0f, 1f)
                                scaleX = 0.9f + 0.1f * p
                                scaleY = 0.9f + 0.1f * p
                                translationY = (1f - p) * 26f
                                alpha = 0.35f + 0.65f * p
                            },
                        )
                    }
                }
                key(top.id) {
                    FilmCard(
                        film = top,
                        vm = vm,
                        swipeProgress = progress,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            translationX = offsetX.value
                            translationY = abs(progress()) * 40f
                            rotationZ = progress() * 12f
                        },
                    )
                }
            }

            SideAction(
                icon = Icons.Filled.Close,
                label = "Skip",
                color = Palette.Red,
                progress = { (-progress() * 3f).coerceIn(0f, 1f) },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 96.dp),
            )
            SideAction(
                icon = Icons.Filled.Favorite,
                label = "Like",
                color = Palette.Pink,
                progress = { (progress() * 3f).coerceIn(0f, 1f) },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 96.dp),
            )

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .glass(RoundedCornerShape(50), Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KeyHint("↓", "Details")
                KeyHint("↑", "Undo")
                KeyHint("▶", "Trailer")
                KeyHint("Back", "Menu")
            }
        }
    }

    LaunchedEffect(top == null) {
        if (top != null) runCatching { focus.requestFocus() }
    }
}

@Composable
private fun FilmCard(
    film: Film,
    vm: AppViewModel,
    modifier: Modifier = Modifier,
    swipeProgress: () -> Float = { 0f },
) {
    val extras = vm.extras[film.id]
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.shadow(36.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFF1C1C1E)),
        ) {
            AsyncImage(
                model = Images.poster(film.poster),
                contentDescription = film.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.42f to Color.Transparent,
                            0.7f to Color.Black.copy(alpha = 0.6f),
                            1f to Color.Black.copy(alpha = 0.96f),
                        )
                    ),
            )
            // Like / skip tint while the card flies away
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (swipeProgress() * 2.5f).coerceIn(0f, 0.55f) }
                    .background(Brush.verticalGradient(listOf(Palette.Pink.copy(alpha = 0.1f), Palette.Pink.copy(alpha = 0.7f)))),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (-swipeProgress() * 2.5f).coerceIn(0f, 0.55f) }
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f)))),
            )

            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)) {
                // Plain title: posters already carry the title art, a logo here would repeat it
                Text(
                    film.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(film.year, film.runtimeLabel, film.genres.take(2).joinToString(", ").ifEmpty { null })
                        .joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    film.imdbRating?.let { ImdbBadge(it) }
                    extras?.rottenTomatoes?.let { ScoreBadge("RT", it, Palette.Tomato) }
                }
            }
        }
        Box(Modifier.fillMaxSize().border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape))

        // Big symbol in the middle while swiping
        Box(
            Modifier
                .align(Alignment.Center)
                .size(110.dp)
                .graphicsLayer {
                    val p = (abs(swipeProgress()) * 3f).coerceIn(0f, 1f)
                    alpha = p
                    scaleX = 0.6f + 0.4f * p
                    scaleY = 0.6f + 0.4f * p
                }
                .background(Color.White.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val liking = swipeProgress() >= 0f
            Icon(
                if (liking) Icons.Filled.Favorite else Icons.Filled.Close,
                contentDescription = null,
                tint = if (liking) Palette.Pink else Palette.Red,
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

@Composable
private fun SideAction(icon: ImageVector, label: String, color: Color, progress: () -> Float, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(84.dp)
                .graphicsLayer {
                    val p = progress()
                    scaleX = 1f + 0.25f * p
                    scaleY = 1f + 0.25f * p
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().glass(CircleShape))
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress() }.background(Color.White, CircleShape))
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = Palette.Secondary)
    }
}

@Composable
private fun KeyHint(key: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            key,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.Secondary)
    }
}

@Composable
private fun EmptyDeck(vm: AppViewModel, sort: SortMode, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    Column(modifier.padding(horizontal = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val error = vm.deckError
        when {
            vm.deckLoading -> {
                Text("Finding movies…", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(sort.label, style = MaterialTheme.typography.bodyLarge, color = Palette.Secondary)
            }
            error != null -> {
                Text("Couldn't load movies", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(error, style = MaterialTheme.typography.bodyLarge, color = Palette.Secondary)
                Spacer(Modifier.height(20.dp))
                PillButton("Try Again", onClick = vm::loadMore, modifier = Modifier.focusRequester(focus))
            }
            vm.deckExhausted -> {
                Text("That's everything for now", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text("Try other genres or another order.", style = MaterialTheme.typography.bodyLarge, color = Palette.Secondary)
                Spacer(Modifier.height(20.dp))
                PillButton("Change Genres", onClick = vm::back, modifier = Modifier.focusRequester(focus))
            }
            else -> {
                Text("No new movies on these pages", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))
                PillButton("Load More", onClick = vm::loadMore, modifier = Modifier.focusRequester(focus))
            }
        }
    }
    LaunchedEffect(vm.deckLoading, vm.deckError, vm.deckExhausted) {
        runCatching { focus.requestFocus() }
    }
}
