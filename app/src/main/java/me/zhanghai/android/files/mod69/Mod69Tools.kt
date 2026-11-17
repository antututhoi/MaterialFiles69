package me.zhanghai.android.files.mod69

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.security.MessageDigest
import java8.nio.file.Paths
import kotlin.concurrent.thread

/**
 * 69 mod: MiXplorer-style "Tools" menu.
 *  - Duplicates: finds files with the same content in the current folder (and its sub-folders).
 *  - Dot file: creates .nomedia / .nosearch / .nothumbnail ... in the current folder.
 *  - Exit: closes the app completely.
 */
object Mod69Tools {
    private val main = Handler(Looper.getMainLooper())

    fun show(context: Context, currentDir: String?, refresh: () -> Unit) {
        val items = arrayOf("Duplicates (this folder and inside it)", "Create a dot file (.nomedia…)", "Exit the app")
        AlertDialog.Builder(context)
            .setTitle("Tools")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> duplicates(context, currentDir, refresh)
                    1 -> dotFile(context, currentDir, refresh)
                    2 -> (context as? Activity)?.finishAffinity()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private val dotNames = arrayOf(".nomedia", ".nosearch", ".nothumbnail", ".nopreview", ".nocache")
    private val dotHelp = arrayOf(
        ".nomedia – gallery and music apps skip this folder",
        ".nosearch – search skips this folder",
        ".nothumbnail – no thumbnails are made here",
        ".nopreview – no folder preview here",
        ".nocache – thumbnails here are not kept",
    )

    private fun dotFile(context: Context, currentDir: String?, refresh: () -> Unit) {
        val dir = currentDir?.takeIf { it.startsWith("/") }?.let { File(it) }
        if (dir == null || !dir.isDirectory) {
            toast(context, "Open a folder on the phone first")
            return
        }
        AlertDialog.Builder(context)
            .setTitle("Dot file in ${dir.name}")
            .setItems(dotHelp) { _, which ->
                val f = File(dir, dotNames[which])
                val ok = runCatching { f.exists() || f.createNewFile() }.getOrDefault(false)
                toast(context, if (ok) "${f.name} is in this folder (turn on hidden files to see it)" else "Couldn't create ${f.name} here")
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private class Dup(val file: File, val group: Int)

    private fun duplicates(context: Context, currentDir: String?, refresh: () -> Unit) {
        val dir = currentDir?.takeIf { it.startsWith("/") }?.let { File(it) }
        if (dir == null || !dir.isDirectory) {
            toast(context, "Open a folder on the phone first")
            return
        }
        var cancelled = false
        val progress = AlertDialog.Builder(context)
            .setTitle("Looking for duplicates")
            .setMessage("Listing files…")
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { _, _ -> cancelled = true }
            .show()
        val started = System.currentTimeMillis()
        thread(name = "mod69-duplicates") {
            // 1. list every file (not the recycle bin), group by size
            val bySize = HashMap<Long, MutableList<File>>()
            var count = 0
            val stack = ArrayDeque<File>().apply { add(dir) }
            while (stack.isNotEmpty() && !cancelled) {
                val d = stack.removeLast()
                val children = d.listFiles() ?: continue
                for (c in children) {
                    if (c.isDirectory) {
                        if (c.name != ".69RecycleBin") stack.add(c)
                    } else if (c.length() > 0) {
                        bySize.getOrPut(c.length()) { ArrayList() }.add(c)
                        count++
                        if (count % 500 == 0) main.post { progress.setMessage("Listing files… $count") }
                    }
                }
            }
            // 2. same size -> compare content (hash)
            val candidates = bySize.values.filter { it.size > 1 }
            val total = candidates.sumOf { it.size }
            var done = 0
            val groups = ArrayList<List<File>>()
            for (same in candidates) {
                if (cancelled) break
                val byHash = HashMap<String, MutableList<File>>()
                for (f in same) {
                    if (cancelled) break
                    done++
                    if (done % 5 == 0) main.post { progress.setMessage("Comparing $done of $total\n${f.name}") }
                    val h = runCatching { hash(f) }.getOrNull() ?: continue
                    byHash.getOrPut(h) { ArrayList() }.add(f)
                }
                byHash.values.filter { it.size > 1 }.forEach { g -> groups.add(g.sortedBy { it.absolutePath.length }) }
            }
            val seconds = (System.currentTimeMillis() - started) / 1000
            main.post {
                runCatching { progress.dismiss() }
                if (cancelled) return@post
                if (groups.isEmpty()) {
                    AlertDialog.Builder(context)
                        .setTitle("No duplicates")
                        .setMessage("Checked $count files in ${seconds}s.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                } else {
                    showResult(context, dir, groups, count, seconds, refresh)
                }
            }
        }
    }

    private fun showResult(
        context: Context, dir: File, groups: List<List<File>>, checked: Int, seconds: Long, refresh: () -> Unit,
    ) {
        val rows = ArrayList<Dup>()
        groups.forEachIndexed { i, g -> g.forEach { rows.add(Dup(it, i)) } }
        // the first (shortest path) of each group is kept, the others are ticked
        val ticked = BooleanArray(rows.size) { i -> i > 0 && rows[i].group == rows[i - 1].group }
        val labels = rows.map { r ->
            val rel = r.file.absolutePath.removePrefix(dir.absolutePath).trimStart('/')
            "#${r.group + 1}  $rel  (${size(r.file.length())})"
        }.toTypedArray()
        val wasted = groups.sumOf { g -> g.drop(1).sumOf { it.length() } }
        AlertDialog.Builder(context)
            .setTitle("${groups.size} duplicate groups · ${size(wasted)} extra")
            .setMultiChoiceItems(labels, ticked) { _, which, isChecked -> ticked[which] = isChecked }
            .setPositiveButton("Delete ticked") { _, _ ->
                val toDelete = rows.filterIndexed { i, _ -> ticked[i] }.map { it.file }
                if (toDelete.isEmpty()) return@setPositiveButton
                val useBin = Mod69Bin.enabled(context)
                thread(name = "mod69-duplicates-delete") {
                    var ok = 0
                    for (f in toDelete) {
                        val moved = useBin && runCatching {
                            Mod69Bin.moveToBin(listOf(Paths.get(f.absolutePath)), context)
                        }.getOrDefault(false)
                        if (moved) {
                            ok++
                            Thread.sleep(3) // each goes into its own bin folder (named by time)
                        } else if (f.delete()) {
                            ok++
                        }
                    }
                    main.post {
                        toast(context, "$ok of ${toDelete.size} duplicates " + if (useBin) "moved to the recycle bin" else "deleted")
                        refresh()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("Checked $checked files in ${seconds}s", null)
            .show()
    }

    /** Whole file for small ones; first + middle + last 1 MB (and the size) for big ones, to stay fast. */
    private fun hash(f: File): String {
        val md = MessageDigest.getInstance("MD5")
        val len = f.length()
        val chunk = 1024 * 1024
        java.io.RandomAccessFile(f, "r").use { raf ->
            val buf = ByteArray(chunk)
            if (len <= 4L * chunk) {
                var n = raf.read(buf)
                while (n > 0) {
                    md.update(buf, 0, n)
                    n = raf.read(buf)
                }
            } else {
                for (pos in longArrayOf(0L, len / 2, len - chunk)) {
                    raf.seek(pos)
                    val n = raf.read(buf)
                    if (n > 0) md.update(buf, 0, n)
                }
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) } + ":" + len
    }

    private fun size(b: Long): String = when {
        b >= 1L shl 30 -> "%.2f GB".format(b / (1L shl 30).toDouble())
        b >= 1L shl 20 -> "%.1f MB".format(b / (1L shl 20).toDouble())
        b >= 1L shl 10 -> "%.0f KB".format(b / 1024.0)
        else -> "$b B"
    }

    private fun toast(context: Context, text: String) =
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
}
