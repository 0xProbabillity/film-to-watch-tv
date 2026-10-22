package com.filmtowatch.tv.ui

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.filmtowatch.tv.data.Provider

/** Opens trailers, streaming apps and system search from the app. */
object Launchers {

    /** TMDB/JustWatch provider id -> Android packages, TV app first. */
    private val PROVIDER_PACKAGES = mapOf(
        8 to listOf("com.netflix.ninja", "com.netflix.mediaclient"),
        1796 to listOf("com.netflix.ninja", "com.netflix.mediaclient"),
        9 to listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
        10 to listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
        119 to listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
        337 to listOf("com.disney.disneyplus"),
        1899 to listOf("com.wbd.stream", "com.hbo.hbonow"),
        384 to listOf("com.wbd.stream", "com.hbo.hbonow"),
        2 to listOf("com.apple.atve.androidtv.appletv"),
        350 to listOf("com.apple.atve.androidtv.appletv"),
        15 to listOf("com.hulu.livingroomplus", "com.hulu.plus"),
        531 to listOf("com.cbs.ott", "com.cbs.app"),
        386 to listOf("com.peacocktv.peacockandroid"),
        387 to listOf("com.peacocktv.peacockandroid"),
        3 to listOf("com.google.android.videos"),
        192 to listOf("com.google.android.youtube.tv", "com.google.android.youtube"),
        283 to listOf("com.crunchyroll.crunchyroid"),
        73 to listOf("com.tubitv"),
        300 to listOf("tv.pluto.android"),
        43 to listOf("com.bydeluxe.d3.android.program.starz"),
    )

    /** True when we know which app opens this service ([hasProviderApp] also requires it to be installed). */
    fun knowsProvider(provider: Provider) = provider.id in PROVIDER_PACKAGES

    fun hasProviderApp(context: Context, provider: Provider): Boolean = PROVIDER_PACKAGES[provider.id].orEmpty().any { pkg ->
        context.packageManager.getLeanbackLaunchIntentForPackage(pkg) != null || context.packageManager.getLaunchIntentForPackage(pkg) != null
    }

    fun playTrailer(context: Context, youtubeId: String?) {
        if (youtubeId == null) {
            toast(context, "No trailer available for this movie")
            return
        }
        val web = Uri.parse("https://www.youtube.com/watch?v=$youtubeId")
        val attempts = listOf(
            Intent(Intent.ACTION_VIEW, web).setPackage("com.google.android.youtube.tv"),
            Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$youtubeId")),
            Intent(Intent.ACTION_VIEW, web),
        )
        if (!startFirst(context, attempts)) toast(context, "Install YouTube to watch trailers")
    }

    fun openProvider(context: Context, provider: Provider, title: String) {
        val packages = PROVIDER_PACKAGES[provider.id].orEmpty()
        val pm = context.packageManager
        for (pkg in packages) {
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: continue
            if (start(context, launch)) {
                toast(context, "Search for \"$title\" in ${provider.name}")
                return
            }
        }
        if (packages.isNotEmpty()) {
            toast(context, "${provider.name} isn't installed")
            start(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${packages.first()}")))
            return
        }
        searchEverywhere(context, "$title ${provider.name}")
    }

    /** Google TV / Android TV global search: shows where a movie can be watched across installed apps. */
    fun searchEverywhere(context: Context, query: String) {
        val attempts = listOf(
            Intent("android.search.action.GLOBAL_SEARCH").putExtra(SearchManager.QUERY, query),
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))),
        )
        if (!startFirst(context, attempts)) toast(context, "Search isn't available on this device")
    }

    private fun startFirst(context: Context, intents: List<Intent>) = intents.any { start(context, it) }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
