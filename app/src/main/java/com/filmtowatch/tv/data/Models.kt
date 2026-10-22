package com.filmtowatch.tv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A movie as the app uses it. Also what we store in the watchlist. */
@Serializable
data class Film(
    val id: String,
    val title: String,
    val year: String? = null,
    val genres: List<String> = emptyList(),
    val overview: String = "",
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val directors: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val awards: String? = null,
    val trailerId: String? = null,
    val tmdbId: Int? = null,
) {
    /** "97 min" -> "1h 37m" */
    val runtimeLabel: String?
        get() = runtime?.filter { it.isDigit() }?.toIntOrNull()?.takeIf { it > 0 }?.let {
            if (it >= 60) "${it / 60}h ${it % 60}m" else "${it}m"
        }
}

/** Metahub serves posters/backgrounds in several sizes; Cinemeta returns the small ones. */
object Images {
    fun poster(url: String?) = url?.replace("/poster/small/", "/poster/medium/")
    fun posterLarge(url: String?) = url?.replace("/poster/small/", "/poster/large/")
    fun backgroundSmall(url: String?) = url?.replace("/background/medium/", "/background/small/")
    fun backgroundLarge(url: String?) = url?.replace("/background/small/", "/background/medium/")
    fun tmdbLogo(path: String?) = path?.let { "https://image.tmdb.org/t/p/w154$it" }
    fun tmdbProfile(path: String?) = path?.let { "https://image.tmdb.org/t/p/w185$it" }
}

// region Cinemeta (Stremio's public metadata catalog, no key needed)

@Serializable
data class CinemetaCatalog(
    val metas: List<CinemetaMeta> = emptyList(),
    val hasMore: Boolean = false,
)

@Serializable
data class CinemetaMeta(
    val id: String,
    val name: String = "",
    val type: String = "",
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val year: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val genres: List<String> = emptyList(),
    val genre: List<String> = emptyList(),
    val director: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val awards: String? = null,
    val trailers: List<CinemetaTrailer> = emptyList(),
    @SerialName("moviedb_id") val tmdbId: Int? = null,
) {
    fun toFilm() = Film(
        id = id,
        title = name,
        year = (year ?: releaseInfo)?.take(4)?.takeIf { it.length == 4 },
        genres = genres.ifEmpty { genre },
        overview = description.orEmpty(),
        poster = poster,
        background = background,
        logo = logo,
        imdbRating = imdbRating?.takeIf { it.isNotBlank() && it != "N/A" },
        runtime = runtime,
        directors = director,
        cast = cast,
        awards = awards?.takeIf { it.isNotBlank() && it != "N/A" },
        trailerId = trailers.firstOrNull { it.type == "Trailer" }?.source ?: trailers.firstOrNull()?.source,
        tmdbId = tmdbId,
    )
}

@Serializable
data class CinemetaTrailer(val source: String, val type: String = "")

val GENRES = listOf(
    "Action", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama", "Family",
    "Fantasy", "History", "Horror", "Mystery", "Romance", "Sci-Fi", "Sport", "Thriller", "War", "Western",
)

enum class SortMode(val label: String) {
    POPULAR("Popular"),
    TOP_RATED("Top Rated"),
    NEW("New Releases"),
}

// endregion

// region TMDB (optional, only for "where to watch" and reviews)

@Serializable
data class TmdbExtras(
    val reviews: ReviewList? = null,
    val credits: TmdbCredits? = null,
    @SerialName("watch/providers") val watchProviders: WatchProviderResults? = null,
)

@Serializable
data class TmdbCredits(
    val cast: List<TmdbPerson> = emptyList(),
    val crew: List<TmdbPerson> = emptyList(),
)

@Serializable
data class TmdbPerson(
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    val character: String? = null,
    val job: String? = null,
)

@Serializable
data class ReviewList(val results: List<Review> = emptyList())

@Serializable
data class Review(
    val author: String = "",
    val content: String = "",
    @SerialName("author_details") val authorDetails: AuthorDetails? = null,
)

@Serializable
data class AuthorDetails(val rating: Double? = null)

@Serializable
data class WatchProviderResults(val results: Map<String, RegionProviders> = emptyMap())

@Serializable
data class RegionProviders(
    val flatrate: List<Provider> = emptyList(),
    val free: List<Provider> = emptyList(),
    val ads: List<Provider> = emptyList(),
    val rent: List<Provider> = emptyList(),
    val buy: List<Provider> = emptyList(),
)

@Serializable
data class Provider(
    @SerialName("provider_id") val id: Int,
    @SerialName("provider_name") val name: String,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int = 0,
)

data class Offer(val provider: Provider, val kind: String)

/** Where to watch in [region], one entry per service, best offer type first. */
fun TmdbExtras.offers(region: String): List<Offer> {
    val r = watchProviders?.results?.get(region) ?: return emptyList()
    val seen = mutableSetOf<Int>()
    val result = mutableListOf<Offer>()
    fun collect(providers: List<Provider>, kind: String) {
        providers.sortedBy { it.displayPriority }.forEach { if (seen.add(it.id)) result += Offer(it, kind) }
    }
    collect(r.flatrate, "Stream")
    collect(r.free, "Free")
    collect(r.ads, "With ads")
    collect(r.rent, "Rent")
    collect(r.buy, "Buy")
    // "Netflix Standard with Ads" / "Amazon Prime Video with Ads" are the same app as the plain entry
    return result.filterNot { offer ->
        val name = offer.provider.name
        name.contains("with ads", ignoreCase = true) &&
            result.any { other -> other !== offer && name.startsWith(other.provider.name, ignoreCase = true) }
    }
}

// endregion

// region Wikipedia (actor photos without a key)

@Serializable
data class WikiResponse(val query: WikiQuery? = null)

@Serializable
data class WikiQuery(val pages: List<WikiPage> = emptyList())

@Serializable
data class WikiPage(val title: String = "", val thumbnail: WikiThumb? = null)

@Serializable
data class WikiThumb(val source: String)

// endregion

// region OMDb (optional, Rotten Tomatoes / Metacritic)

@Serializable
data class OmdbResponse(
    @SerialName("Metascore") val metascore: String? = null,
    @SerialName("Ratings") val ratings: List<OmdbRating> = emptyList(),
    @SerialName("Response") val response: String? = null,
)

@Serializable
data class OmdbRating(
    @SerialName("Source") val source: String,
    @SerialName("Value") val value: String,
)

// endregion

/** Optional extras for the details screen, each null when its key isn't configured or the call failed. */
data class FilmExtras(
    val tmdb: TmdbExtras? = null,
    val rottenTomatoes: String? = null,
    val metacritic: String? = null,
)

data class Region(val code: String, val name: String)

val REGIONS = listOf(
    Region("US", "United States"),
    Region("GB", "United Kingdom"),
    Region("IE", "Ireland"),
    Region("DE", "Germany"),
    Region("AT", "Austria"),
    Region("CH", "Switzerland"),
    Region("FR", "France"),
    Region("BE", "Belgium"),
    Region("NL", "Netherlands"),
    Region("LU", "Luxembourg"),
    Region("ES", "Spain"),
    Region("PT", "Portugal"),
    Region("IT", "Italy"),
    Region("PL", "Poland"),
    Region("CZ", "Czechia"),
    Region("SK", "Slovakia"),
    Region("HU", "Hungary"),
    Region("RO", "Romania"),
    Region("BG", "Bulgaria"),
    Region("GR", "Greece"),
    Region("HR", "Croatia"),
    Region("SI", "Slovenia"),
    Region("RS", "Serbia"),
    Region("SE", "Sweden"),
    Region("NO", "Norway"),
    Region("DK", "Denmark"),
    Region("FI", "Finland"),
    Region("EE", "Estonia"),
    Region("LV", "Latvia"),
    Region("LT", "Lithuania"),
    Region("CY", "Cyprus"),
)
