package me.zhanghai.android.files.mod69

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import java8.nio.file.Path
import java8.nio.file.Paths
import java.io.File

/** 69 mod: remembers the folder of the last pick so the next chooser opens there. */
object Mod69LastPick {
    private const val PREFS = "mod69_pick"
    private const val KEY = "last_dir"

    fun save(context: Context, dir: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, dir).apply()
    }

    fun initialPath(context: Context, intent: Intent?): Path? {
        // The calling app can ask for a start folder (EXTRA_INITIAL_URI); use it when it is one of ours
        // or a normal storage uri like content://com.android.externalstorage.documents/tree/primary:Pictures
        val initial: Uri? = if (Build.VERSION.SDK_INT >= 26) {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI)
        } else {
            null
        }
        fromUri(initial)?.let { return it }
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        val f = File(last)
        return if (f.isDirectory) Paths.get(last) else null
    }

    private fun fromUri(uri: Uri?): Path? {
        if (uri == null) return null
        return try {
            val id = if (DocumentsContract.isTreeUri(uri)) {
                DocumentsContract.getTreeDocumentId(uri)
            } else {
                DocumentsContract.getDocumentId(uri)
            }
            val path = when {
                id.startsWith("/") -> id
                id.startsWith("primary:") -> "/storage/emulated/0/" + id.removePrefix("primary:")
                else -> null
            } ?: return null
            if (File(path).isDirectory) Paths.get(path) else null
        } catch (e: Exception) {
            null
        }
    }
}
