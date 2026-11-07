package me.zhanghai.android.files.mod69

import android.content.Context
import androidx.lifecycle.MutableLiveData
import me.zhanghai.android.files.app.application

/** 69 mod: recently opened folders, shown in the side bar. */
object Mod69Recent {
    private const val PREFS = "mod69_recent"
    private const val KEY = "dirs"
    private const val MAX = 8

    val liveData = MutableLiveData(load().also { Mod69Look.visited += it })

    private fun prefs() = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun load(): List<String> =
        prefs().getString(KEY, null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    fun add(path: String) {
        if (path.isBlank()) return
        Mod69Look.visited += path
        val list = (listOf(path) + (liveData.value ?: emptyList()).filter { it != path }).take(MAX)
        if (list == liveData.value) return
        prefs().edit().putString(KEY, list.joinToString("\n")).apply()
        liveData.value = list
    }

    fun remove(path: String) {
        val list = (liveData.value ?: emptyList()).filter { it != path }
        prefs().edit().putString(KEY, list.joinToString("\n")).apply()
        liveData.value = list
    }
}
