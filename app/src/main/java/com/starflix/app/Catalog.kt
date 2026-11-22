package com.starflix.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.StringReader

data class CEp(
    val ep: Int,
    val title: String?,
    @SerializedName(value = "stream", alternate = ["url", "link", "m3u8"]) val stream: String,
)
data class CSeason(val season: Int, val episodes: List<CEp>?)
data class CMovie(
    @SerializedName(value = "tmdb_id", alternate = ["id", "tmdbId", "tmdb"]) val tmdb_id: Int,
    @SerializedName(value = "stream", alternate = ["url", "link", "m3u8"]) val stream: String,
    val title: String? = null,   // optional: used only if TMDB can't be reached
    val poster: String? = null,  // optional: full image URL, same fallback
)
data class CSeries(
    @SerializedName(value = "tmdb_id", alternate = ["id", "tmdbId", "tmdb"]) val tmdb_id: Int,
    val seasons: List<CSeason>?,
    val title: String? = null,
    val poster: String? = null,
)
data class CatalogFile(val movies: List<CMovie>?, val series: List<CSeries>?)

data class Genre(val id: Int, val name: String?)
data class Detail(
    val id: Int, val title: String?, val name: String?, val poster_path: String?, val vote_average: Double?,
    val release_date: String?, val first_air_date: String?, val overview: String?, val adult: Boolean?, val genres: List<Genre>?,
) {
    fun toTitle(tv: Boolean) = Title(id, title, name, poster_path, vote_average, release_date, first_air_date,
        if (tv) "tv" else "movie", genres?.map { it.id }, overview, adult)
}

/** Your own library: stream links come from catalog.json, everything else (poster, rating...) from TMDB. */
object Catalog {
    var movies: Map<Int, String> = emptyMap()
    var series: Map<Int, CSeries> = emptyMap()
    /** Human-readable reason when something is wrong (shown on the Home screen). Null = all good. */
    var problem: String? = null

    private fun parse(json: String): CatalogFile =
        Gson().fromJson(JsonReader(StringReader(json)).apply { isLenient = true }, CatalogFile::class.java) ?: CatalogFile(null, null)

    suspend fun load(ctx: Context): List<Title> = withContext(Dispatchers.IO) {
        val notes = mutableListOf<String>()
        var c: CatalogFile? = null
        if (BuildConfig.CATALOG_URL.isNotBlank()) {
            try {
                val json = OkHttpClient().newCall(Request.Builder().url(BuildConfig.CATALOG_URL).build()).execute().use {
                    if (!it.isSuccessful) error("HTTP ${it.code}"); it.body!!.string()
                }
                c = parse(json)
            } catch (e: Exception) { notes += "CATALOG_URL failed (${e.message}); using bundled catalog.json" }
        }
        if (c == null) {
            try { c = parse(ctx.assets.open("catalog.json").bufferedReader().use { it.readText() }) }
            catch (e: Exception) { problem = "catalog.json is not valid JSON: ${e.message}"; return@withContext emptyList() }
        }
        val cat = c!!
        movies = cat.movies.orEmpty().associate { it.tmdb_id to it.stream }
        series = cat.series.orEmpty().associateBy { it.tmdb_id }
        val failed = mutableListOf<Int>()
        val titles = coroutineScope {
            val m = cat.movies.orEmpty().map { x -> async {
                runCatching { api.movieDetail(x.tmdb_id).toTitle(false) }.getOrNull()
                    ?: x.title?.let { Title(x.tmdb_id, it, null, x.poster, null, null, null, "movie", null, null, null) }
                    ?: run { failed += x.tmdb_id; null }
            } }
            val s = cat.series.orEmpty().map { x -> async {
                runCatching { api.tvDetail(x.tmdb_id).toTitle(true) }.getOrNull()
                    ?: x.title?.let { Title(x.tmdb_id, null, it, x.poster, null, null, null, "tv", null, null, null) }
                    ?: run { failed += x.tmdb_id; null }
            } }
            (m + s).awaitAll().filterNotNull()
        }
        if (failed.isNotEmpty()) notes += "TMDB couldn't load id ${failed.joinToString()}. Check the tmdb_id (movie vs tv) and your API token."
        if (cat.movies.isNullOrEmpty() && cat.series.isNullOrEmpty()) notes += "catalog.json has no movies or series."
        problem = notes.takeIf { it.isNotEmpty() }?.joinToString("\n")
        titles
    }
}
