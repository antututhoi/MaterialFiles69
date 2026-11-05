package me.zhanghai.android.files.mod69

import android.content.Context
import android.os.Environment
import androidx.preference.PreferenceManager
import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.filejob.FileJobService
import org.json.JSONObject
import java.io.File

/**
 * 69 mod: recycle bin. Deleted files are moved into Internal/.69RecycleBin/<time>/ and the original
 * folder is written to .69restore.json there, so "Restore" can move them back.
 */
object Mod69Bin {
    const val PREF_ENABLED = "mod69_recycle_bin"
    private const val INDEX = ".69restore.json"

    val root: File get() = File(Environment.getExternalStorageDirectory(), ".69RecycleBin")

    fun enabled(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, true)

    fun isInBin(path: Path): Boolean = path.toString().startsWith(root.absolutePath)

    /** Returns false when the bin can't be used (then delete normally). */
    fun moveToBin(paths: List<Path>, context: Context): Boolean {
        if (paths.isEmpty() || paths.any { isInBin(it) || !it.toString().startsWith("/") }) return false
        val dir = File(root, System.currentTimeMillis().toString())
        if (!dir.mkdirs()) return false
        val index = JSONObject()
        paths.forEach { p -> index.put(p.fileName.toString(), p.parent?.toString() ?: "") }
        File(dir, INDEX).writeText(index.toString())
        File(root, ".nomedia").takeIf { !it.exists() }?.createNewFile()
        FileJobService.move(paths, Paths.get(dir.absolutePath), context)
        return true
    }

    /** Moves the given bin items back to where they came from. */
    fun restore(paths: List<Path>, context: Context): Int {
        var n = 0
        paths.groupBy { it.parent?.toString() ?: "" }.forEach { (parent, list) ->
            val index = runCatching { JSONObject(File(parent, INDEX).readText()) }.getOrNull() ?: return@forEach
            list.groupBy { index.optString(it.fileName.toString(), "") }.forEach { (target, items) ->
                if (target.isNotEmpty()) {
                    File(target).mkdirs()
                    FileJobService.move(items, Paths.get(target), context)
                    n += items.size
                }
            }
        }
        return n
    }

    fun emptyBin(context: Context) {
        val children = root.listFiles()?.map { Paths.get(it.absolutePath) } ?: return
        if (children.isNotEmpty()) FileJobService.delete(children, context)
    }
}
