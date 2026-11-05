package me.zhanghai.android.files.mod69

/** 69 mod: a search to start once the file list shows the next folder (from the side bar shortcuts). */
object Mod69PendingSearch {
    @Volatile var query: String? = null

    fun take(): String? = query.also { query = null }
}
