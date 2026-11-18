package me.zhanghai.android.files.mod69

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * 69 mod (MiXplorer folder rows "date | 12 items"): counts what is inside a folder in the background
 * and adds it to the row. Only for folders on the phone (not archives / network / SAF).
 */
object Mod69FolderInfo {
    private val main = Handler(Looper.getMainLooper())
    private val pool = Executors.newFixedThreadPool(2)
    // path -> (folder modified time, text)
    private val cache = ConcurrentHashMap<String, Pair<Long, String>>()

    fun bind(view: TextView, path: String, dateText: String?, modified: Long, showHidden: Boolean) {
        view.tag = path
        val base = dateText.orEmpty()
        if (!path.startsWith("/")) {
            view.text = base.ifEmpty { null }
            return
        }
        val key = "$path|$showHidden"
        cache[key]?.takeIf { it.first == modified }?.let {
            view.text = join(base, it.second)
            return
        }
        view.text = base.ifEmpty { null }
        pool.execute {
            val names = runCatching { File(path).list() }.getOrNull()
            val text = if (names == null) {
                ""
            } else {
                val n = if (showHidden) names.size else names.count { !it.startsWith(".") }
                if (n == 1) "1 item" else "$n items"
            }
            cache[key] = modified to text
            main.post { if (view.tag == path) view.text = join(base, text) }
        }
    }

    private fun join(a: String, b: String): String? = listOf(a, b).filter { it.isNotEmpty() }.joinToString(" | ").ifEmpty { null }
}
