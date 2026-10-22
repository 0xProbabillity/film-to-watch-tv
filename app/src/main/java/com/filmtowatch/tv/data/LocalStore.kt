package com.filmtowatch.tv.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.Executors

@Serializable
data class StoreData(
    val watchlist: List<Film> = emptyList(),
    /** IMDb ids of movies already swiped (liked or skipped), so they don't show up again. */
    val seen: Set<String> = emptySet(),
    val region: String? = null,
    val genres: Set<String> = emptySet(),
    val sort: SortMode = SortMode.POPULAR,
)

/** Tiny JSON-file store: everything lives in memory and is written to disk after each change. */
class LocalStore(context: Context) {
    private val file = File(context.filesDir, "store_v2.json")
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }
    private val writer = CoroutineScope(SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val _data = MutableStateFlow(load())
    val data: StateFlow<StoreData> = _data

    fun update(transform: (StoreData) -> StoreData) {
        _data.updateAndGet(transform)
        writer.launch { save(_data.value) }
    }

    private fun load(): StoreData = runCatching {
        if (file.exists()) json.decodeFromString(StoreData.serializer(), file.readText()) else StoreData()
    }.getOrDefault(StoreData())

    private fun save(data: StoreData) {
        runCatching {
            val tmp = File(file.parentFile, "store_v2.json.tmp")
            tmp.writeText(json.encodeToString(StoreData.serializer(), data))
            tmp.renameTo(file)
        }
    }
}
