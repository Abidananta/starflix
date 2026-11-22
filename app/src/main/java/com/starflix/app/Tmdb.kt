package com.starflix.app

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class Title(
    val id: Int,
    val title: String?, val name: String?,
    val poster_path: String?,
    val vote_average: Double?,
    val release_date: String?, val first_air_date: String?,
    val media_type: String?,
    val genre_ids: List<Int>?,
    val overview: String?, val adult: Boolean?,
) {
    val displayTitle get() = title ?: name ?: ""
    val year get() = (release_date ?: first_air_date ?: "").take(4)
    val poster get() = poster_path?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" }
    val posterLarge get() = poster_path?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w780$it" }
    val rating get() = "%.1f".format(vote_average ?: 0.0)
    val kind get() = if (media_type == "tv" || first_air_date != null) "SERIES" else "MOVIE"
    val genreList get() = (genre_ids ?: emptyList()).mapNotNull { GENRES[it] }.distinct().take(3)
    val recType get() = if (kind == "SERIES") "tv" else "movie"
    val genres get() = (genre_ids ?: emptyList()).mapNotNull { GENRES[it] }.take(3).joinToString("  •  ")
}
data class Page(val results: List<Title>)

interface Api {
    @GET("trending/all/week") suspend fun trending(): Page
    @GET("movie/popular") suspend fun popular(): Page
    @GET("tv/popular") suspend fun popularTv(): Page
    @GET("{type}/{id}/recommendations") suspend fun recs(@Path("type") type: String, @Path("id") id: Int): Page
    @GET("movie/{id}") suspend fun movieDetail(@Path("id") id: Int): Detail
    @GET("tv/{id}") suspend fun tvDetail(@Path("id") id: Int): Detail
    @GET("movie/top_rated") suspend fun topRated(): Page
}

// TMDB "API Read Access Token" (v4, starts with eyJ...) is sent as a Bearer header.
private val client = OkHttpClient.Builder().addInterceptor { chain ->
    chain.proceed(chain.request().newBuilder()
        .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY}")
        .addHeader("accept", "application/json").build())
}.build()

val api: Api = Retrofit.Builder().baseUrl("https://api.themoviedb.org/3/").client(client)
    .addConverterFactory(GsonConverterFactory.create()).build().create(Api::class.java)

val GENRES = mapOf(28 to "Action", 12 to "Adventure", 16 to "Animation", 35 to "Comedy", 80 to "Crime",
    99 to "Documentary", 18 to "Drama", 10751 to "Family", 14 to "Fantasy", 27 to "Horror", 9648 to "Mystery",
    10749 to "Romance", 878 to "Sci-Fi", 53 to "Thriller", 10759 to "Action", 10765 to "Sci-Fi", 10768 to "War")
