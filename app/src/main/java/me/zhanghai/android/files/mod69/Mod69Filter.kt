package me.zhanghai.android.files.mod69

import android.content.Context
import androidx.appcompat.app.AlertDialog
import me.zhanghai.android.files.file.FileItem
import me.zhanghai.android.files.file.isApk
import me.zhanghai.android.files.file.isAudio
import me.zhanghai.android.files.file.isImage
import me.zhanghai.android.files.file.isSupportedArchive
import me.zhanghai.android.files.file.isVideo

/**
 * 69 mod (MiXplorer main menu "Filter"): show only one kind of file in the current folder.
 * Folders stay visible. The filter is cleared when you open another folder.
 */
object Mod69Filter {
    val types = listOf("Images", "Videos", "Audio", "Documents", "Archives", "APKs")

    fun matches(type: String, file: FileItem): Boolean {
        if (file.attributes.isDirectory) return true
        val m = file.mimeType
        return when (type) {
            "Images" -> m.isImage
            "Videos" -> m.isVideo
            "Audio" -> m.isAudio
            "Archives" -> m.isSupportedArchive && !m.isApk
            "APKs" -> m.isApk
            "Documents" -> {
                val v = m.value
                v.startsWith("text/") || v == "application/pdf" || v == "application/epub+zip" || v == "application/rtf" ||
                    listOf("msword", "document", "spreadsheet", "presentation", "opendocument", "ms-excel", "ms-powerpoint")
                        .any { it in v }
            }
            else -> true
        }
    }

    fun dialog(context: Context, current: String?, set: (String?) -> Unit) {
        val items = (types + "Show everything").toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Filter this folder")
            .setSingleChoiceItems(items, current?.let { types.indexOf(it) } ?: types.size) { d, which ->
                set(if (which >= types.size) null else types[which])
                d.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
