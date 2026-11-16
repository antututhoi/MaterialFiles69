package me.zhanghai.android.files.mod69

import android.view.Menu
import me.zhanghai.android.files.R
import me.zhanghai.android.files.file.FileItem

/**
 * 69 mod: MiXplorer-style extra entries in a file's menu:
 * Copy name, Open in new tab (folders), Hide / Unhide (adds or removes the leading dot), Open as (text / image / audio / video / any type).
 */
object Mod69FileActions {
    fun prepare(menu: Menu, file: FileItem, readOnly: Boolean, picking: Boolean) {
        fun item(id: Int, order: Int, title: String, visible: Boolean) {
            val it = menu.findItem(id) ?: menu.add(Menu.NONE, id, order, title)
            it.title = title
            it.isVisible = visible
        }
        val isDir = file.attributes.isDirectory
        val hidden = file.path.fileName?.toString()?.startsWith(".") == true
        item(R.id.mod69_file_open_as, 1, "Open as…", !isDir && !picking)
        item(R.id.mod69_file_new_tab, 2, "Open in new tab", isDir && !picking)
        item(R.id.mod69_file_copy_name, 90, "Copy name", true)
        item(R.id.mod69_file_hide, 91, if (hidden) "Unhide" else "Hide", !readOnly && !picking)
    }

    /** New name for Hide / Unhide. */
    fun toggledHiddenName(name: String): String = if (name.startsWith(".")) name.trimStart('.') else ".$name"

}
