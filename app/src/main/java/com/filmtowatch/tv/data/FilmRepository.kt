package com.filmtowatch.tv.data

import com.filmtowatch.tv.BuildConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface CinemetaApi {
    @GET
    suspend fun catalog(@Url url: String): CinemetaCatalog
}

interface TmdbApi {
    @GET("movie/{id}")
    suspend fun extras(
        @Path("id") id: Int,
        @Query("append_to_response", encoded = true) append: String = "reviews,credits,watch/providers",
        @Query("language") language: String = "en-US",
    ): TmdbExtras
}

interface WikipediaApi {
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=search&gsrlimit=1&prop=pageimages&piprop=thumbnail&pithumbsize=300")
    suspend fun search(@Query("gsrsearch") query: String): WikiResponse
}

interface OmdbApi {
    @GET("/")
    suspend fun byImdbId(@Query("i") imdbId: String, @Query("apikey") apiKey: String): OmdbResponse
}

/** One paged Cinemeta catalog, e.g. "top movies in Sci-Fi". */
data class CatalogQuery(val catalog: String, val genre: String?) {
    fun url(skip: Int): String {
        val extras = listOfNotNull(genre?.let { "genre=$it" }, skip.takeIf { it > 0 }?.let { "skip=$it" })
        val suffix = if (extras.isEmpty()) "" else "/" + extras.joinToString("&")
        return "https://v3-cinemeta.strem.io/catalog/movie/$catalog$suffix.json"
    }
}

class FilmRepository private constructor(
    private val cinemeta: CinemetaApi,
    private val tmdb: TmdbApi,
    private val omdb: OmdbApi,
    private val wikipedia: WikipediaApi,
    private val omdbKey: String,
    val hasTmdb: Boolean,
) {
    val hasOmdb: Boolean get() = omdbKey.isNotEmpty()

    suspend fun catalog(query: CatalogQuery, skip: Int): CinemetaCatalog = cinemeta.catalog(query.url(skip))

    /** Streaming availability + reviews (TMDB) and RT/Metacritic (OMDb), whichever keys are configured. */
    suspend fun extras(film: Film): FilmExtras = coroutineScope {
        val tmdbCall = async {
            if (hasTmdb && film.tmdbId != null) runCatching { tmdb.extras(film.tmdbId) }.getOrNull() else null
        }
        val omdbCall = async {
            if (hasOmdb) runCatching { omdb.byImdbId(film.id, omdbKey) }.getOrNull()?.takeIf { it.response != "False" } else null
        }
        val omdbResult = omdbCall.await()
        fun String?.clean() = this?.takeIf { it.isNotBlank() && it != "N/A" }
        FilmExtras(
            tmdb = tmdbCall.await(),
            rottenTomatoes = omdbResult?.ratings?.firstOrNull { it.source == "Rotten Tomatoes" }?.value.clean(),
            metacritic = omdbResult?.metascore.clean(),
        )
    }

    /** Photo of a cast/crew member from Wikipedia, or null when no confident match is found. */
    suspend fun personPhoto(name: String, isDirector: Boolean): String? {
        val query = "$name ${if (isDirector) "film director" else "actor"}"
        val page = runCatching { wikipedia.search(query) }.getOrNull()?.query?.pages?.firstOrNull() ?: return null
        val lastName = name.trim().substringAfterLast(' ').lowercase()
        return page.thumbnail?.source?.takeIf { page.title.lowercase().contains(lastName) }
    }

    companion object {
        /** Wikimedia asks API clients to identify themselves. */
        val USER_AGENT = "FilmToWatchTV/${BuildConfig.VERSION_NAME} (https://github.com/0xProbabillity/film-to-watch-tv)"

        fun create(): FilmRepository {
            val tmdbKey = BuildConfig.TMDB_API_KEY
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            }
            val converter = json.asConverterFactory("application/json".toMediaType())

            // TMDB accepts either a v3 API key (query param) or a v4 read access token (a JWT, sent as Bearer)
            val tmdbAuth = Interceptor { chain ->
                val request = chain.request()
                val authed = if (tmdbKey.startsWith("eyJ")) {
                    request.newBuilder().header("Authorization", "Bearer $tmdbKey").build()
                } else {
                    request.newBuilder()
                        .url(request.url.newBuilder().addQueryParameter("api_key", tmdbKey).build())
                        .build()
                }
                chain.proceed(authed)
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            fun retrofit(baseUrl: String, httpClient: OkHttpClient) = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(converter)
                .build()

            return FilmRepository(
                cinemeta = retrofit("https://v3-cinemeta.strem.io/", client).create(CinemetaApi::class.java),
                tmdb = retrofit("https://api.themoviedb.org/3/", client.newBuilder().addInterceptor(tmdbAuth).build())
                    .create(TmdbApi::class.java),
                omdb = retrofit("https://www.omdbapi.com/", client).create(OmdbApi::class.java),
                wikipedia = retrofit(
                    "https://en.wikipedia.org/",
                    client.newBuilder()
                        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build()) }
                        .build(),
                ).create(WikipediaApi::class.java),
                omdbKey = BuildConfig.OMDB_API_KEY,
                hasTmdb = tmdbKey.isNotEmpty(),
            )
        }
    }
}
