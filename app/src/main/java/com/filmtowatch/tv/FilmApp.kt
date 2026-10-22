package com.filmtowatch.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.filmtowatch.tv.data.FilmRepository
import okhttp3.OkHttpClient

class FilmApp : Application(), ImageLoaderFactory {
    // Wikimedia rejects image requests without a descriptive User-Agent
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().header("User-Agent", FilmRepository.USER_AGENT).build())
                }
                .build()
        }
        .crossfade(true)
        .build()
}
