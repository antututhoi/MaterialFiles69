package me.zhanghai.android.files.mod69

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.customview.widget.ViewDragHelper
import androidx.drawerlayout.widget.DrawerLayout

/** 69 mod: a wider left-edge zone to swipe the side bar open, also with gesture navigation. */
object Mod69Edge {
    fun widen(drawer: DrawerLayout) {
        val density = drawer.resources.displayMetrics.density
        try {
            val f = DrawerLayout::class.java.getDeclaredField("mLeftDragger")
            f.isAccessible = true
            val dragger = f.get(drawer) as ViewDragHelper
            val edge = ViewDragHelper::class.java.getDeclaredField("mEdgeSize")
            edge.isAccessible = true
            edge.setInt(dragger, (48 * density).toInt())
        } catch (_: Throwable) {
        }
        if (Build.VERSION.SDK_INT >= 29) {
            // the system lets an app take back up to 200dp of the back-gesture edge
            drawer.addOnLayoutChangeListener { v: View, _, _, _, _, _, _, _, _ ->
                val h = (200 * density).toInt()
                val top = (v.height - h) / 2
                v.systemGestureExclusionRects = listOf(Rect(0, top, (48 * density).toInt(), top + h))
            }
        }
    }
}
