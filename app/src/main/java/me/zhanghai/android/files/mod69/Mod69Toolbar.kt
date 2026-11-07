package me.zhanghai.android.files.mod69

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.application

/**
 * 69 mod: choose which actions sit as buttons on the top bar (the rest stay in the ⋮ menu).
 * Adds "Two panels", "Recycle bin" and "Recent" actions too.
 */
object Mod69Toolbar {
    const val ID_DUAL = 0x69a1
    const val ID_BIN = 0x69a2
    const val ID_HOME = 0x69a3

    private class Action(val key: String, val id: Int, val name: String)

    private val actions = listOf(
        Action("search", R.id.action_search, "Search"),
        Action("view_sort", R.id.action_view_sort, "View / sort"),
        Action("refresh", R.id.action_refresh, "Refresh"),
        Action("hidden", R.id.action_show_hidden_files, "Show hidden files"),
        Action("select_all", R.id.action_select_all, "Select all"),
        Action("new_task", R.id.action_new_task, "New window"),
        Action("navigate_up", R.id.action_navigate_up, "Up one folder"),
        Action("navigate_to", R.id.action_navigate_to, "Go to path"),
        Action("bookmark", R.id.action_add_bookmark, "Add bookmark"),
        Action("copy_path", R.id.action_copy_path, "Copy path"),
        Action("dual", ID_DUAL, "Two panels"),
        Action("bin", ID_BIN, "Recycle bin"),
        Action("home", ID_HOME, "Internal storage"),
    )

    private val prefs get() = application.getSharedPreferences("mod69_toolbar", Context.MODE_PRIVATE)

    /** 69 mod: MiXplorer-style: the chosen buttons sit in a bar at the bottom. */
    val atBottom: Boolean get() = prefs.getBoolean("bottom", false)

    fun chosenIds(): List<Int> = chosen().let { set -> actions.filter { it.key in set }.map { it.id } }

    private fun chosen(): Set<String> = prefs.getStringSet("buttons", setOf("search", "view_sort")) ?: emptySet()

    /** Adds the extra actions and applies the button choice. Call after the menu is inflated. */
    fun apply(menu: Menu) {
        if (menu.findItem(ID_DUAL) == null) {
            menu.add(Menu.NONE, ID_DUAL, 200, "Two panels").setIcon(R.drawable.computer_icon_white_24dp)
            menu.add(Menu.NONE, ID_BIN, 201, "Recycle bin").setIcon(R.drawable.delete_icon_control_normal_24dp)
            menu.add(Menu.NONE, ID_HOME, 202, "Internal storage").setIcon(R.drawable.device_icon_white_24dp)
            menu.add(Menu.NONE, 0x69a9, 203, "Toolbar buttons…")
        }
        val set = chosen()
        for (a in actions) {
            val item = menu.findItem(a.id) ?: continue
            item.setShowAsAction(
                if (a.key in set && !atBottom) MenuItem.SHOW_AS_ACTION_ALWAYS else MenuItem.SHOW_AS_ACTION_NEVER
            )
        }
    }

    /** Handles the extra actions. Returns true when handled. */
    fun onItem(context: Context, item: MenuItem, navigate: (String) -> Unit, invalidate: () -> Unit): Boolean =
        when (item.itemId) {
            ID_DUAL -> {
                context.startActivity(Mod69DualActivity.intent(context)); true
            }
            ID_BIN -> {
                navigate(Mod69Bin.root.apply { mkdirs() }.absolutePath); true
            }
            ID_HOME -> {
                navigate(android.os.Environment.getExternalStorageDirectory().absolutePath); true
            }
            0x69a9 -> {
                showChooser(context, invalidate); true
            }
            else -> false
        }

    private fun showChooser(context: Context, invalidate: () -> Unit) {
        val set = chosen().toMutableSet()
        val names = actions.map { it.name }.toTypedArray()
        val checked = actions.map { it.key in set }.toBooleanArray()
        AlertDialog.Builder(context)
            .setTitle("Toolbar buttons")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                if (isChecked) set.add(actions[which].key) else set.remove(actions[which].key)
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                prefs.edit().putStringSet("buttons", set).apply()
                invalidate()
            }
            .setNeutralButton(if (atBottom) "Move to top" else "Move to bottom") { _, _ ->
                prefs.edit().putStringSet("buttons", set).putBoolean("bottom", !atBottom).apply()
                invalidate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
