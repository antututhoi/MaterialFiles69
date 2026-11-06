package me.zhanghai.android.files.mod69

import android.content.ClipData
import android.content.Context
import android.view.DragEvent
import android.view.View
import android.widget.PopupMenu
import java8.nio.file.Path
import me.zhanghai.android.files.filejob.FileJobService

/** 69 mod: hold a selected file and drag it onto a folder (or the other panel) to copy or move. */
object Mod69Drag {
    class State(val paths: List<Path>)

    fun start(view: View, paths: List<Path>): Boolean {
        if (paths.isEmpty()) return false
        val clip = ClipData.newPlainText("files", paths.joinToString("\n"))
        return view.startDragAndDrop(clip, CountShadow(view, paths.size), State(paths), 0)
    }

    /** The dragged picture: the item, with a round badge showing how many items are dragged. */
    private class CountShadow(view: View, private val count: Int) : View.DragShadowBuilder(view) {
        private val d = view.resources.displayMetrics.density
        private val badge = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE53935.toInt()
        }
        private val text = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 14 * d
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        override fun onProvideShadowMetrics(size: android.graphics.Point, touch: android.graphics.Point) {
            val v = view
            size.set(v.width.coerceAtMost((360 * d).toInt()), v.height)
            touch.set(size.x / 2, size.y / 2)
        }

        override fun onDrawShadow(canvas: android.graphics.Canvas) {
            canvas.save()
            canvas.saveLayerAlpha(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), 200)
            view.draw(canvas)
            canvas.restore()
            canvas.restore()
            val r = 14 * d
            val cx = canvas.width - r - 4 * d
            val cy = r + 4 * d
            canvas.drawCircle(cx, cy, r, badge)
            canvas.drawText(if (count > 99) "99+" else count.toString(), cx, cy - (text.descent() + text.ascent()) / 2, text)
        }
    }

    /** The folder under the finger gets a coloured overlay. */
    private fun highlight(v: View, on: Boolean) {
        if (on) {
            if (v.getTag(KEEP) == null) v.setTag(KEEP, arrayOf(v.foreground))
            val tv = android.util.TypedValue()
            val color = if (v.context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)) tv.data else 0xFF2196F3.toInt()
            v.foreground = android.graphics.drawable.ColorDrawable((color and 0x00FFFFFF) or 0x55000000)
        } else {
            @Suppress("UNCHECKED_CAST")
            val kept = v.getTag(KEEP) as? Array<android.graphics.drawable.Drawable?> ?: return
            v.foreground = kept[0]
            v.setTag(KEEP, null)
        }
    }

    private val KEEP = me.zhanghai.android.files.R.id.mod69_drag_keep

    /** Makes [view] accept drops; [target] gives the folder to drop into (null = not a folder). */
    fun makeDropTarget(view: View, target: () -> Path?) {
        view.setOnDragListener { v, e ->
            val state = e.localState as? State ?: return@setOnDragListener false
            val dir = target() ?: return@setOnDragListener false
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    highlight(v, true)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    highlight(v, false)
                    true
                }
                DragEvent.ACTION_DROP -> {
                    highlight(v, false)
                    if (state.paths.any { it == dir || it.parent == dir }) {
                        // dropped on itself / its own folder: nothing to do
                        return@setOnDragListener false
                    }
                    ask(v.context, v, state.paths, dir)
                    true
                }
                else -> true
            }
        }
    }

    private fun ask(context: Context, anchor: View, paths: List<Path>, dir: Path) {
        PopupMenu(context, anchor).apply {
            menu.add("Copy ${paths.size} here").setOnMenuItemClickListener {
                FileJobService.copy(paths, dir, context); true
            }
            menu.add("Move ${paths.size} here").setOnMenuItemClickListener {
                FileJobService.move(paths, dir, context); true
            }
            show()
        }
    }
}
