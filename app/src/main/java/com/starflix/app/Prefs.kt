package com.starflix.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import com.google.gson.Gson

object Prefs {
    private lateinit var sp: SharedPreferences
    var accent by mutableIntStateOf(0xFF5B7CFF.toInt())
    var textScale by mutableFloatStateOf(1f)
    var quality by mutableIntStateOf(0) // 0 = Auto, else max height
    var adultFilter by mutableStateOf(true)
    val favorites = mutableStateListOf<Title>()

    fun init(c: Context) {
        sp = c.getSharedPreferences("starflix", 0)
        accent = sp.getInt("accent", accent); textScale = sp.getFloat("ts", 1f)
        quality = sp.getInt("q", 0); adultFilter = sp.getBoolean("adult", true)
        runCatching { favorites.addAll(Gson().fromJson(sp.getString("fav", "[]"), Array<Title>::class.java)) }
    }
    fun save() = sp.edit().putInt("accent", accent).putFloat("ts", textScale).putInt("q", quality)
        .putBoolean("adult", adultFilter).putString("fav", Gson().toJson(favorites.toList())).apply()
    fun isFav(t: Title) = favorites.any { it.id == t.id }
    fun toggleFav(t: Title) { if (isFav(t)) favorites.removeAll { it.id == t.id } else favorites.add(0, t); save() }
}
