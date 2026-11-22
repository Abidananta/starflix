package com.starflix.app

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class CEp(val ep: Int, val title: String?, val stream: String)
data class CSeason(val season: Int, val episodes: List<CEp>?)
data class CMovie(val tmdb_id: Int, val stream: String)
data class CSeries(val tmdb_id: Int, val seasons: List<CSeason>?)
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

    suspend fun load(ctx: Context): List<Title> = withContext(Dispatchers.IO) {
        val remote = runCatching {
            if (BuildConfig.CATALOG_URL.isBlank()) null
            else OkHttpClient().newCall(Request.Builder().url(BuildConfig.CATALOG_URL).build()).execute().use {
                if (!it.isSuccessful) error("http ${it.code}"); it.body!!.string()
            }
        }.getOrNull()
        val json = remote ?: ctx.assets.open("catalog.json").bufferedReader().use { it.readText() }
        val c = Gson().fromJson(json, CatalogFile::class.java)
        movies = c.movies.orEmpty().associate { it.tmdb_id to it.stream }
        series = c.series.orEmpty().associateBy { it.tmdb_id }
        coroutineScope {
            val m = c.movies.orEmpty().map { x -> async { runCatching { api.movieDetail(x.tmdb_id).toTitle(false) }.getOrNull() } }
            val s = c.series.orEmpty().map { x -> async { runCatching { api.tvDetail(x.tmdb_id).toTitle(true) }.getOrNull() } }
            (m + s).awaitAll().filterNotNull()
        }
    }
}
