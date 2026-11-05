package me.zhanghai.android.files.mod69

import android.content.Context
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.google.android.material.chip.Chip
import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.app.application

/**
 * 69 mod: folder tabs under the path bar. Tap a tab to go there, "+" adds the current folder,
 * hold a tab to close it. The open tab follows you while you browse.
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

    fun render() {
        row.removeAllViews()
        val gap = (4 * context.resources.displayMetrics.density).toInt()
        tabs.forEachIndexed { i, p ->
            row.addView(
                Chip(context).apply {
                    text = Paths.get(p).fileName?.toString() ?: p
                    isCheckable = true
                    isChecked = i == active
                    setOnClickListener {
                        active = i
                        save()
                        navigate(Paths.get(p))
                        render()
                    }
                    setOnLongClickListener {
                        tabs.removeAt(i)
                        if (active >= tabs.size) active = (tabs.size - 1).coerceAtLeast(0)
                        save()
                        render()
                        true
                    }
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginEnd = gap },
            )
        }
        row.addView(
            Chip(context).apply {
                text = if (tabs.isEmpty()) "+ Tab" else "+"
                setOnClickListener {
                    tabs.add(currentPath().toString())
                    active = tabs.size - 1
                    save()
                    render()
                }
            },
        )
    }
}
