package me.zhanghai.android.files.mod69

import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

/**
 * 69 mod: MiXplorer-style toolbar at the bottom of the file list: the chosen buttons in a row,
 * and ⋮ with everything else. The top bar keeps the title and its own ⋮.
 */
class Mod69BottomTools(
    private val host: ViewGroup,
    private val list: RecyclerView,
    private val fab: View?,
    private val onItem: (MenuItem) -> Boolean,
) {
    private val context = host.context
    private val d = context.resources.displayMetrics.density
    private val barHeight = (52 * d).toInt()
    private var listPadding = -1
    private val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(MaterialColors.getColor(host, com.google.android.material.R.attr.colorSurface, 0xFF000000.toInt()))
        elevation = 6 * d
    }

    init {
        host.addView(
            row,
            CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barHeight).apply {
                gravity = Gravity.BOTTOM
            },
        )
        row.visibility = View.GONE
        // keep above the phone's navigation bar
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(row) { v, insets ->
            val b = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
            (v.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = b
            v.requestLayout()
            insets
        }
    }

    fun isIn(parent: ViewGroup) = parent === host

    fun render(menu: Menu) {
        if (!Mod69Toolbar.atBottom) {
            if (row.visibility != View.GONE) {
                row.visibility = View.GONE
                fab?.translationY = 0f
                if (listPadding >= 0) list.setPadding(list.paddingLeft, list.paddingTop, list.paddingRight, listPadding)
            }
            return
        }
        row.visibility = View.VISIBLE
        if (listPadding < 0) listPadding = list.paddingBottom
        list.setPadding(list.paddingLeft, list.paddingTop, list.paddingRight, listPadding + barHeight)
        list.clipToPadding = false
        fab?.translationY = -barHeight.toFloat()
        row.removeAllViews()
        val tint = MaterialColors.getColor(row, androidx.appcompat.R.attr.colorControlNormal, 0xFFFFFFFF.toInt())
        val ids = Mod69Toolbar.chosenIds()
        ids.mapNotNull { menu.findItem(it) }.filter { it.isVisible && it.icon != null }.forEach { item ->
            row.addView(button(item.icon!!.constantState?.newDrawable()?.mutate() ?: item.icon!!, item.title, tint) { v ->
                if (item.hasSubMenu()) popup(v, item.subMenu!!) else onItem(item)
            })
        }
        // ⋮ = everything
        row.addView(
            button(
                androidx.appcompat.content.res.AppCompatResources.getDrawable(
                    context, androidx.appcompat.R.drawable.abc_ic_menu_overflow_material,
                )!!,
                "More", tint,
            ) { v -> popup(v, menu) },
        )
    }

    private fun button(icon: android.graphics.drawable.Drawable, label: CharSequence?, tint: Int, click: (View) -> Unit) =
        ImageButton(context).apply {
            setImageDrawable(icon.apply { setTint(tint) })
            contentDescription = label
            tooltipText = label
            val tv = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)
            setBackgroundResource(tv.resourceId)
            setOnClickListener { click(it) }
            layoutParams = LinearLayout.LayoutParams(0, barHeight, 1f)
        }

    /** A copy of [menu] (or a sub-menu) as a popup; picking an entry runs the real one. */
    private fun popup(anchor: View, menu: Menu) {
        val pm = PopupMenu(context, anchor)
        val map = HashMap<Int, MenuItem>()
        var n = 0
        for (i in 0 until menu.size()) {
            val it = menu.getItem(i)
            if (!it.isVisible) continue
            val id = ++n
            map[id] = it
            val copy = pm.menu.add(it.groupId, id, i, it.title)
            copy.isCheckable = it.isCheckable
            copy.isChecked = it.isChecked
            copy.isEnabled = it.isEnabled
        }
        pm.setOnMenuItemClickListener { c ->
            val real = map[c.itemId] ?: return@setOnMenuItemClickListener false
            if (real.hasSubMenu()) popup(anchor, real.subMenu!!) else onItem(real)
            true
        }
        pm.show()
    }
}
