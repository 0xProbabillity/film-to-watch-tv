package com.filmtowatch.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.filmtowatch.tv.data.Film
import com.filmtowatch.tv.data.FilmExtras
import com.filmtowatch.tv.data.Images
import com.filmtowatch.tv.data.Offer
import com.filmtowatch.tv.data.REGIONS
import com.filmtowatch.tv.data.Review
import com.filmtowatch.tv.data.offers
import kotlinx.coroutines.launch

private val PagePadding = 64.dp

private data class Person(val name: String, val role: String, val photo: String?)

/**
 * The page scrolls section by section (see [sectionFocus]), so the list itself must not react to
 * focus changes; rows inside get the default TV behaviour back.
 */
@OptIn(ExperimentalFoundationApi::class)
private val NoAutoScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/** Horizontal rows: scroll only when the focused item gets close to an edge, so the first item never gets cut. */
@OptIn(ExperimentalFoundationApi::class)
private val RowScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val margin = containerSize * 0.07f
        val trailingEdge = offset + size
        return when {
            offset < margin -> offset - margin
            trailingEdge > containerSize - margin -> trailingEdge - (containerSize - margin)
            else -> 0f
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailsScreen(vm: AppViewModel, screen: Screen.Details) {
    val film = vm.film(screen.filmId)
    if (film == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This movie is no longer available", style = MaterialTheme.typography.headlineSmall, color = Palette.Secondary)
        }
        return
    }
    LaunchedEffect(film.id) {
        vm.ensureExtras(film)
        vm.ensurePhotos(film)
    }

    val context = LocalContext.current
    val data by vm.data.collectAsStateWithLifecycle()
    val extras = vm.extras[film.id]
    val region = data.region ?: vm.region
    val offers = remember(extras, region) { extras?.tmdb?.offers(region).orEmpty() }
    val reviews = remember(extras) { extras?.tmdb?.reviews?.results.orEmpty().take(10) }
    val people = people(film, extras, vm.personPhotos)
    val inWatchlist = data.watchlist.any { it.id == film.id }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val firstFocus = remember { FocusRequester() }
    val rowSpec = RowScroll
    var openReview by remember { mutableStateOf<Review?>(null) }

    /** When focus enters a section, scroll so the whole section sits at the top of the screen. */
    fun Modifier.sectionFocus(index: Int) = onFocusChanged {
        if (it.hasFocus) scope.launch { listState.animateScrollToItem(index) }
    }

    Box(Modifier.fillMaxSize().background(Palette.Background)) {
        AsyncImage(
            model = Images.backgroundLarge(film.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.92f),
                        0.45f to Color.Black.copy(alpha = 0.6f),
                        0.8f to Color.Transparent,
                    )
                ),
        )
        // Darken more as the page scrolls so rows stay readable over bright backdrops
        val scrolled = listState.firstVisibleItemIndex > 0
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.3f to Color.Black.copy(alpha = if (scrolled) 0.75f else 0f),
                        1f to Color.Black.copy(alpha = 0.95f),
                    )
                ),
        )

        CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var index = 0

                val heroIndex = index++
                item(key = "hero") {
                    Column(Modifier.padding(horizontal = PagePadding).padding(top = 40.dp).sectionFocus(heroIndex)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Box(Modifier.weight(1f)) { Hero(film, extras) }
                            Spacer(Modifier.width(40.dp))
                            PosterArt(film)
                        }
                        Spacer(Modifier.height(26.dp))
                        // With TMDB data the best way to watch comes first: "Watch on Netflix"
                        // Prefer a service whose app is on this TV, then one we know how to open
                        val bestOffer = remember(offers) {
                            offers.firstOrNull { Launchers.hasProviderApp(context, it.provider) }
                                ?: offers.firstOrNull { Launchers.knowsProvider(it.provider) }
                                ?: offers.firstOrNull()
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (bestOffer != null) {
                                WatchButton(
                                    bestOffer,
                                    onClick = { Launchers.openProvider(context, bestOffer.provider, film.title) },
                                    modifier = Modifier.focusRequester(firstFocus),
                                )
                            }
                            PillButton(
                                if (film.trailerId != null) (if (bestOffer != null) "Trailer" else "Play Trailer") else "No Trailer",
                                onClick = { Launchers.playTrailer(context, film.trailerId) },
                                icon = Icons.Filled.PlayArrow,
                                large = true,
                                modifier = if (bestOffer == null) Modifier.focusRequester(firstFocus) else Modifier,
                            )
                            if (screen.fromSwipe) {
                                PillButton("Like", onClick = { vm.decideFromDetails(film.id, like = true) }, icon = Icons.Filled.Favorite, iconTint = Palette.Pink, large = true)
                                PillButton("Skip", onClick = { vm.decideFromDetails(film.id, like = false) }, icon = Icons.Filled.Close, iconTint = Palette.Red, large = true)
                            } else if (inWatchlist) {
                                PillButton("Remove from Watchlist", onClick = { vm.removeFromWatchlist(film.id); vm.back() }, icon = Icons.Filled.Check, large = true)
                            } else {
                                PillButton("Add to Watchlist", onClick = { vm.addToWatchlist(film) }, icon = Icons.Filled.Favorite, iconTint = Palette.Pink, large = true)
                            }
                            if (bestOffer == null) {
                                PillButton(
                                    "Find on TV",
                                    onClick = { Launchers.searchEverywhere(context, listOfNotNull(film.title, film.year).joinToString(" ")) },
                                    icon = Icons.Filled.Search,
                                    large = true,
                                )
                            }
                        }
                        Spacer(Modifier.height(40.dp))
                    }
                }

                if (offers.isNotEmpty()) {
                    val sectionIndex = index++
                    item(key = "watch") {
                        Column(Modifier.sectionFocus(sectionIndex)) {
                            val regionName = REGIONS.firstOrNull { it.code == region }?.name ?: region
                            RowHeader("Where to Watch", "$regionName · availability by JustWatch")
                            CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                                    contentPadding = PaddingValues(horizontal = PagePadding, vertical = 16.dp),
                                ) {
                                    items(offers, key = { it.provider.id }) { offer ->
                                        ProviderIcon(offer) { Launchers.openProvider(context, offer.provider, film.title) }
                                    }
                                }
                            }
                        }
                    }
                }

                if (people.isNotEmpty()) {
                    val sectionIndex = index++
                    item(key = "people") {
                        Column(Modifier.sectionFocus(sectionIndex)) {
                            RowHeader("Cast & Crew", "Select a person to search for their movies")
                            CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                                    contentPadding = PaddingValues(horizontal = PagePadding, vertical = 16.dp),
                                ) {
                                    items(people, key = { it.name + it.role }) { person ->
                                        PersonBubble(person) { Launchers.searchEverywhere(context, person.name) }
                                    }
                                }
                            }
                        }
                    }
                }

                if (reviews.isNotEmpty()) {
                    val sectionIndex = index++
                    item(key = "reviews") {
                        Column(Modifier.sectionFocus(sectionIndex)) {
                            RowHeader("Reviews", "Select a review to read it in full")
                            CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    contentPadding = PaddingValues(horizontal = PagePadding, vertical = 16.dp),
                                ) {
                                    itemsIndexed(reviews) { _, review -> ReviewCard(review) { openReview = review } }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = openReview != null, enter = fadeIn(), exit = fadeOut()) {
            openReview?.let { ReviewOverlay(it, onClose = { openReview = null }) }
        }
    }

    LaunchedEffect(film.id) { runCatching { firstFocus.requestFocus() } }
    // Streaming data arrives a moment later: move focus to "Watch on …" if the user is still at the top
    val hasWatchButton = offers.isNotEmpty()
    LaunchedEffect(hasWatchButton) {
        if (hasWatchButton && listState.firstVisibleItemIndex == 0 && openReview == null) runCatching { firstFocus.requestFocus() }
    }
}

/** Cast & crew with photos: TMDB credits when a key is set (full cast, character names), otherwise Cinemeta + Wikipedia. */
private fun people(film: Film, extras: FilmExtras?, wikiPhotos: Map<String, String>): List<Person> {
    val credits = extras?.tmdb?.credits
    if (credits != null && credits.cast.isNotEmpty()) {
        val directors = credits.crew.filter { it.job == "Director" }
            .map { Person(it.name, "Director", Images.tmdbProfile(it.profilePath) ?: wikiPhotos[it.name]) }
        val cast = credits.cast.take(15)
            .map { Person(it.name, it.character?.takeIf { c -> c.isNotBlank() } ?: "Actor", Images.tmdbProfile(it.profilePath) ?: wikiPhotos[it.name]) }
        return (directors + cast).distinctBy { it.name + it.role }
    }
    return film.directors.map { Person(it, "Director", wikiPhotos[it]) } + film.cast.map { Person(it, "Actor", wikiPhotos[it]) }
}

@Composable
private fun Hero(film: Film, extras: FilmExtras?) {
    var logoFailed by remember(film.id) { mutableStateOf(false) }
    Column(Modifier.widthIn(max = 600.dp)) {
        if (film.logo != null && !logoFailed) {
            AsyncImage(
                model = film.logo,
                contentDescription = film.title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                onError = { logoFailed = true },
                modifier = Modifier.widthIn(max = 420.dp).heightIn(max = 110.dp),
            )
        } else {
            Text(film.title, style = MaterialTheme.typography.displayMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            film.imdbRating?.let { ImdbBadge(it) }
            extras?.rottenTomatoes?.let { ScoreBadge("Rotten Tomatoes", it, Palette.Tomato) }
            extras?.metacritic?.let { ScoreBadge("Metacritic", it, Palette.Metacritic) }
            val meta = listOfNotNull(film.year, film.runtimeLabel).joinToString("  ·  ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.titleSmall, color = Palette.Secondary)
            film.genres.take(3).forEach { OutlineBadge(it) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            film.overview.ifBlank { "No description available." },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        val credits = listOfNotNull(
            film.cast.takeIf { it.isNotEmpty() }?.let { "Starring ${it.take(3).joinToString(", ")}" },
            film.directors.takeIf { it.isNotEmpty() }?.let { "Directed by ${it.joinToString(", ")}" },
        )
        if (credits.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(credits.joinToString("   ·   "), style = MaterialTheme.typography.bodyMedium, color = Palette.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        film.awards?.let {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Palette.Imdb, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Palette.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Primary "Watch on <service>" capsule with the service's logo. */
@Composable
private fun WatchButton(offer: Offer, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val verb = when (offer.kind) {
        "Rent" -> "Rent on"
        "Buy" -> "Buy on"
        else -> "Watch on"
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        scale = ButtonDefaults.scale(focusedScale = 1.07f),
        colors = ButtonDefaults.colors(
            containerColor = Palette.GlassStrong,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        contentPadding = PaddingValues(start = 14.dp, end = 28.dp, top = 12.dp, bottom = 12.dp),
    ) {
        AsyncImage(
            model = Images.tmdbLogo(offer.provider.logoPath),
            contentDescription = null,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "$verb ${offer.provider.name}",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

/** The full poster, uncropped, as a floating card on the right of the hero. */
@Composable
private fun PosterArt(film: Film) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .width(220.dp)
            .height(330.dp)
            .shadow(28.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Color(0xFF1C1C1E))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape),
    ) {
        AsyncImage(
            model = Images.posterLarge(film.poster),
            contentDescription = film.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun RowHeader(title: String, subtitle: String) {
    Row(Modifier.padding(horizontal = PagePadding).padding(top = 40.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(14.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.Tertiary, modifier = Modifier.padding(bottom = 3.dp))
    }
}

/** Streaming service shown as an app icon. */
@Composable
private fun ProviderIcon(offer: Offer, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.width(104.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(96.dp),
            shape = ClickableSurfaceDefaults.shape(shape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF2C2C2E), focusedContainerColor = Color(0xFF2C2C2E)),
            glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(elevationColor = Color.Black, elevation = 16.dp)),
        ) {
            AsyncImage(
                model = Images.tmdbLogo(offer.provider.logoPath),
                contentDescription = offer.provider.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(offer.provider.name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(offer.kind, style = MaterialTheme.typography.labelSmall, color = Palette.Tertiary)
    }
}

/** Round portrait (photo, or initials while it loads / when none exists), Apple TV "Cast & Crew" style. */
@Composable
private fun PersonBubble(person: Person, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val initials = person.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    val hue = ((person.name.hashCode() and 0xFFFFFF) % 360).toFloat()
    Column(Modifier.width(128.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(112.dp).onFocusChanged { focused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(elevationColor = Color.Black, elevation = 14.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color.hsv(hue, 0.35f, 0.55f), Color.hsv((hue + 40f) % 360f, 0.45f, 0.3f)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                if (person.photo != null) {
                    AsyncImage(
                        model = person.photo,
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            person.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (focused) Color.White else Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(person.role, style = MaterialTheme.typography.labelMedium, color = Palette.Tertiary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReviewCard(review: Review, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(440.dp).height(210.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(elevationColor = Color.Black, elevation = 16.dp)),
    ) {
        Column(Modifier.padding(22.dp)) {
            ReviewHeader(review)
            Spacer(Modifier.height(10.dp))
            Text(review.cleanContent(), style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ReviewHeader(review: Review) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(review.author, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        review.authorDetails?.rating?.let {
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Filled.Star, contentDescription = null, tint = Palette.Imdb, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("${formatRating(it)}/10", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Full review in a frosted panel; ↑/↓ scroll the text, Back closes. */
@Composable
private fun ReviewOverlay(review: Review, onClose: () -> Unit) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.82f)
                .glass(RoundedCornerShape(28.dp), Color(0xFF1C1C1E))
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> scope.launch { scroll.animateScrollBy(260f) }
                        Key.DirectionUp -> scope.launch { scroll.animateScrollBy(-260f) }
                        Key.DirectionCenter, Key.Enter -> onClose()
                        // Keep focus inside the panel
                        Key.DirectionLeft, Key.DirectionRight -> Unit
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                }
                .focusable()
                .padding(36.dp),
        ) {
            ReviewHeader(review)
            Spacer(Modifier.height(16.dp))
            Text(
                review.cleanContent(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f).verticalScroll(scroll),
            )
            Spacer(Modifier.height(14.dp))
            Text("↑ ↓ scroll   ·   OK or Back to close", style = MaterialTheme.typography.labelMedium, color = Palette.Tertiary)
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

private fun Review.cleanContent() = content.replace(Regex("[*_#>]+"), "").replace(Regex("\\n{3,}"), "\n\n").trim()
