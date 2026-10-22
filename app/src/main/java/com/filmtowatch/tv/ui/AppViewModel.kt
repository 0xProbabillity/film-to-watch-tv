package com.filmtowatch.tv.ui

import android.app.Application
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filmtowatch.tv.data.CatalogQuery
import com.filmtowatch.tv.data.Film
import com.filmtowatch.tv.data.FilmExtras
import com.filmtowatch.tv.data.FilmRepository
import com.filmtowatch.tv.data.GENRES
import com.filmtowatch.tv.data.Images
import com.filmtowatch.tv.data.LocalStore
import com.filmtowatch.tv.data.REGIONS
import com.filmtowatch.tv.data.SortMode
import com.filmtowatch.tv.data.StoreData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Year
import java.util.Locale

sealed interface Screen {
    data object Home : Screen
    data object Swipe : Screen
    data object Support : Screen
    data class Details(val filmId: String, val fromSwipe: Boolean) : Screen
}

enum class HomeTab(val label: String) { Discover("Discover"), Watchlist("Watchlist") }

/** A paged catalog feeding the deck, optionally filtered by genre on our side. */
private class DeckSource(val query: CatalogQuery, val genreFilter: Set<String>?) {
    var skip = 0
    var hasMore = true
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val repo = FilmRepository.create()

    val data: StateFlow<StoreData> = store.data
    val region: String get() = data.value.region ?: defaultRegion()
    val hasStreamingData: Boolean get() = repo.hasTmdb

    val backStack = mutableStateListOf<Screen>(Screen.Home)
    var homeTab by mutableStateOf(HomeTab.Discover)

    /** True when Home is (re)entered and should put focus into its content; switching tabs keeps focus on the tab bar. */
    var restoreHomeFocus = true

    /** Genre -> background art used on the genre tiles. */
    val genreArt = mutableStateMapOf<String, String>()
    var catalogError by mutableStateOf<String?>(null)
        private set

    val deck = mutableStateListOf<Film>()
    var deckLoading by mutableStateOf(false)
        private set
    var deckExhausted by mutableStateOf(false)
        private set
    var deckError by mutableStateOf<String?>(null)
        private set
    private var sources = emptyList<DeckSource>()
    private var newReleasesMode = false
    private var deckJob: Job? = null
    private val history = ArrayDeque<Pair<Film, Boolean>>()

    /** Every film we've shown this session, so the details screen can find it by id. */
    private val films = HashMap<String, Film>()
    val extras = mutableStateMapOf<String, FilmExtras>()
    private val extrasLoading = mutableSetOf<String>()

    /** Person name -> photo URL (from Wikipedia). */
    val personPhotos = mutableStateMapOf<String, String>()
    private val photosRequested = mutableSetOf<String>()

    /** Kept here so the watchlist keeps its scroll position and focus after opening a movie. */
    val watchlistGridState = LazyGridState()
    var lastOpenedFromWatchlist by mutableStateOf<String?>(null)

    init {
        loadGenreArt()
    }

    // region Navigation

    fun navigate(screen: Screen) {
        if (screen is Screen.Details && !screen.fromSwipe) lastOpenedFromWatchlist = screen.filmId
        backStack.add(screen)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        if (backStack[backStack.lastIndex] == Screen.Home) restoreHomeFocus = true
    }

    // endregion

    // region Home

    fun loadGenreArt() {
        catalogError = null
        viewModelScope.launch {
            try {
                val top = repo.catalog(CatalogQuery("top", null), 0).metas.map { it.toFilm() }
                val used = mutableSetOf<String>()
                for (genre in GENRES) {
                    top.firstOrNull { genre in it.genres && it.background != null && it.id !in used }?.let {
                        used += it.id
                        genreArt[genre] = Images.backgroundSmall(it.background)!!
                    }
                }
                GENRES.filter { it !in genreArt }.map { genre ->
                    async {
                        runCatching { repo.catalog(CatalogQuery("top", genre), 0) }.getOrNull()
                            ?.metas?.map { it.toFilm() }
                            ?.firstOrNull { it.background != null && it.id !in used }
                            ?.let { genre to Images.backgroundSmall(it.background)!! }
                    }
                }.awaitAll().filterNotNull().forEach { (genre, art) -> genreArt[genre] = art }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                catalogError = "Couldn't reach the movie catalog. Check your internet connection."
            }
        }
    }

    fun toggleGenre(genre: String) = store.update { it.copy(genres = if (genre in it.genres) it.genres - genre else it.genres + genre) }

    fun setSort(sort: SortMode) = store.update { it.copy(sort = sort) }

    fun setRegion(code: String) = store.update { it.copy(region = code) }

    fun resetHistory() = store.update { it.copy(seen = emptySet()) }

    // endregion

    // region Swiping

    fun startSwiping() {
        val s = data.value
        val selected = GENRES.filter { it in s.genres }
        newReleasesMode = s.sort == SortMode.NEW
        sources = when (s.sort) {
            SortMode.POPULAR, SortMode.TOP_RATED -> {
                val catalog = if (s.sort == SortMode.POPULAR) "top" else "imdbRating"
                if (selected.isEmpty()) listOf(DeckSource(CatalogQuery(catalog, null), null))
                else selected.map { DeckSource(CatalogQuery(catalog, it), null) }
            }
            // Cinemeta's year catalog can't filter by genre, so we filter ourselves
            SortMode.NEW -> {
                val year = Year.now().value
                (0..2).map { DeckSource(CatalogQuery("year", (year - it).toString()), selected.toSet().ifEmpty { null }) }
            }
        }
        deckJob?.cancel()
        deck.clear()
        history.clear()
        deckExhausted = false
        deckError = null
        deckLoading = false
        navigate(Screen.Swipe)
        loadMore()
    }

    fun loadMore() {
        if (deckJob?.isActive == true || deckExhausted || deck.size >= DECK_LOW_WATER) return
        deckJob = viewModelScope.launch {
            deckLoading = true
            deckError = null
            try {
                var rounds = 0
                while (deck.size < DECK_TARGET && rounds < MAX_ROUNDS_PER_LOAD) {
                    val active = sources.filter { it.hasMore }
                    if (active.isEmpty()) {
                        deckExhausted = true
                        break
                    }
                    // Several genres: pull a page from each and interleave. New releases: go year by year.
                    val batch = if (newReleasesMode) active.take(1) else active
                    val pages = batch.map { src -> async { src to repo.catalog(src.query, src.skip) } }.awaitAll()
                        .map { (src, page) ->
                            src.skip += page.metas.size
                            src.hasMore = page.hasMore && page.metas.isNotEmpty()
                            page.metas.map { it.toFilm() }
                                .filter { film -> src.genreFilter == null || film.genres.any { it in src.genreFilter } }
                        }
                    val seen = data.value.seen
                    val known = deck.mapTo(mutableSetOf()) { it.id }
                    for (i in 0 until (pages.maxOfOrNull { it.size } ?: 0)) {
                        for (page in pages) {
                            val film = page.getOrNull(i) ?: continue
                            if (film.poster != null && film.id !in seen && known.add(film.id)) {
                                films[film.id] = film
                                deck += film
                            }
                        }
                    }
                    rounds++
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deckError = e.message ?: e.javaClass.simpleName
            } finally {
                deckLoading = false
            }
            prefetchExtras()
        }
    }

    fun swipe(like: Boolean) {
        val film = deck.removeFirstOrNull() ?: return
        history.addLast(film to like)
        if (history.size > 50) history.removeFirst()
        store.update { s ->
            s.copy(
                seen = s.seen + film.id,
                watchlist = if (like) listOf(film) + s.watchlist.filterNot { it.id == film.id } else s.watchlist,
            )
        }
        loadMore()
        prefetchExtras()
    }

    /** Brings back the last swiped card. Returns false when there is nothing to undo. */
    fun undo(): Boolean {
        val (film, liked) = history.removeLastOrNull() ?: return false
        deck.add(0, film)
        store.update { s ->
            s.copy(
                seen = s.seen - film.id,
                watchlist = if (liked) s.watchlist.filterNot { it.id == film.id } else s.watchlist,
            )
        }
        return true
    }

    /** Like/skip pressed on the details screen opened from the swipe deck. */
    fun decideFromDetails(filmId: String, like: Boolean) {
        if (deck.firstOrNull()?.id == filmId) {
            swipe(like)
        } else if (like) {
            film(filmId)?.let { addToWatchlist(it) }
        }
        back()
    }

    private fun prefetchExtras() {
        if (repo.hasOmdb) deck.take(2).forEach { ensureExtras(it) }
    }

    // endregion

    // region Details & watchlist

    fun film(id: String): Film? = films[id] ?: data.value.watchlist.firstOrNull { it.id == id }

    fun ensureExtras(film: Film) {
        if (!repo.hasTmdb && !repo.hasOmdb) return
        if (film.id in extras || !extrasLoading.add(film.id)) return
        viewModelScope.launch {
            try {
                extras[film.id] = repo.extras(film)
            } finally {
                extrasLoading.remove(film.id)
            }
        }
    }

    fun ensurePhotos(film: Film) {
        val people = film.directors.map { it to true } + film.cast.map { it to false }
        people.forEach { (name, isDirector) ->
            if (photosRequested.add(name)) {
                viewModelScope.launch { repo.personPhoto(name, isDirector)?.let { personPhotos[name] = it } }
            }
        }
    }

    fun addToWatchlist(film: Film) = store.update { s ->
        s.copy(watchlist = listOf(film) + s.watchlist.filterNot { it.id == film.id }, seen = s.seen + film.id)
    }

    fun removeFromWatchlist(id: String) = store.update { s -> s.copy(watchlist = s.watchlist.filterNot { it.id == id }) }

    // endregion

    private fun defaultRegion(): String {
        val country = Locale.getDefault().country.uppercase(Locale.US)
        return if (REGIONS.any { it.code == country }) country else "US"
    }

    private companion object {
        const val DECK_LOW_WATER = 6
        const val DECK_TARGET = 15
        const val MAX_ROUNDS_PER_LOAD = 4
    }
}
