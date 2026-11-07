package me.zhanghai.android.files.mod69

import android.content.Context
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.app.application

/**
 * 69 mod: MiXplorer-style folder tabs under the path bar. Tap a tab to go there, "+" adds the current folder,
 * hold a tab for the tab menu (close, close others / left / right, move, clone, set as home). The open tab follows you while you browse.
 */
class Mod69Tabs(
    private val context: Context,
    private val currentPath: () -> Path,
    private val navigate: (Path) -> Unit,
) {
    private val prefs = application.getSharedPreferences("mod69_tabs", Context.MODE_PRIVATE)
    private var tabs: MutableList<String> =
        prefs.getString("tabs", null)?.split('\n')?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
    private var active = prefs.getInt("active", 0)
    private val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    val view = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(row)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val pad = (8 * context.resources.displayMetrics.density).toInt()
        setPadding(pad, 0, pad, 0)
    }

    private fun save() {
        prefs.edit().putString("tabs", tabs.joinToString("\n")).putInt("active", active).apply()
    }

    /** Called when the folder changes: the open tab follows. */
    fun onPathChanged(path: Path) {
        if (tabs.isEmpty()) return
        val p = path.toString()
        if (active in tabs.indices && tabs[active] != p) {
            tabs[active] = p
            save()
        }
        render()
    }

    private fun go(i: Int) {
        if (i !in tabs.indices) return
        active = i
        save()
        navigate(Paths.get(tabs[i]))
        render()
    }

    /** MiXplorer-like tab menu (hold a tab). */
    private fun menu(anchor: android.view.View, i: Int) {
        val pm = android.widget.PopupMenu(context, anchor)
        val items = listOf(
            "Close tab", "Close other tabs", "Close tabs on the left", "Close tabs on the right",
            "Move left", "Move right", "Clone tab", "Set as home folder", "Close all tabs",
        )
        items.forEachIndexed { n, t -> pm.menu.add(0, n, n, t) }
        pm.setOnMenuItemClickListener { item ->
            val cur = tabs.getOrNull(active)
            when (item.itemId) {
                0 -> tabs.removeAt(i)
                1 -> tabs = mutableListOf(tabs[i])
                2 -> repeat(i) { tabs.removeAt(0) }
                3 -> while (tabs.size > i + 1) tabs.removeAt(tabs.size - 1)
                4 -> if (i > 0) java.util.Collections.swap(tabs, i, i - 1)
                5 -> if (i < tabs.size - 1) java.util.Collections.swap(tabs, i, i + 1)
                6 -> tabs.add(i + 1, tabs[i])
                7 -> {
                    me.zhanghai.android.files.settings.Settings.FILE_LIST_DEFAULT_DIRECTORY.putValue(Paths.get(tabs[i]))
                    android.widget.Toast.makeText(context, "Home folder set", android.widget.Toast.LENGTH_SHORT).show()
                }
                8 -> tabs.clear()
            }
            active = when (item.itemId) {
                1 -> 0
                4 -> if (active == i && i > 0) i - 1 else if (active == i - 1) i else active
                5 -> if (active == i && i < tabs.size - 1) i + 1 else if (active == i + 1) i else active
                6 -> i + 1
                else -> cur?.let { tabs.indexOf(it) }?.takeIf { it >= 0 } ?: active.coerceAtMost(tabs.size - 1)
            }.coerceAtLeast(0)
            save()
            render()
            true
        }
        pm.show()
    }

    fun render() {
        row.removeAllViews()
        val d = context.resources.displayMetrics.density
        val accent = com.google.android.material.color.MaterialColors.getColor(
            view, androidx.appcompat.R.attr.colorPrimary, 0xFF00E676.toInt(),
        )
        val normal = com.google.android.material.color.MaterialColors.getColor(
            view, android.R.attr.textColorSecondary, 0xFFAAAAAA.toInt(),
        )
        // MiXplorer-style tabs: plain names, the open one in the accent colour with a line under it
        fun tab(label: String, selected: Boolean): android.widget.TextView = android.widget.TextView(context).apply {
            text = label
            textSize = 14f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            maxWidth = (160 * d).toInt()
            setTextColor(if (selected) accent else normal)
            if (selected) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
            background = if (selected) {
                android.graphics.drawable.LayerDrawable(
                    arrayOf(android.graphics.drawable.ColorDrawable(accent)),
                ).apply { setLayerInset(0, (6 * d).toInt(), 0, (6 * d).toInt(), 0); setLayerGravity(0, android.view.Gravity.BOTTOM); setLayerHeight(0, (3 * d).toInt()) }
            } else {
                null
            }
        }
        tabs.forEachIndexed { i, p ->
            row.addView(
                tab(Paths.get(p).fileName?.toString() ?: p, i == active).apply {
                    setOnClickListener { go(i) }
                    setOnLongClickListener {
                        menu(this, i)
                        true
                    }
                },
            )
        }
        row.addView(
            tab(if (tabs.isEmpty()) "+ Tab" else "+", false).apply {
                setOnClickListener {
                    tabs.add(currentPath().toString())
                    active = tabs.size - 1
                    save()
                    render()
                }
            },
        )
        if (active in tabs.indices) row.post { row.getChildAt(active)?.let { view.smoothScrollTo((it.left - 24 * d).toInt(), 0) } }
    }
}
