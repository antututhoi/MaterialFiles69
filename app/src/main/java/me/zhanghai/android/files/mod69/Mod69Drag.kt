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
        return view.startDragAndDrop(clip, View.DragShadowBuilder(view), State(paths), 0)
    }

    /** Makes [view] accept drops; [target] gives the folder to drop into (null = not a folder). */
    fun makeDropTarget(view: View, target: () -> Path?) {
        view.setOnDragListener { v, e ->
            val state = e.localState as? State ?: return@setOnDragListener false
            val dir = target() ?: return@setOnDragListener false
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    v.alpha = 0.5f
                    true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    v.alpha = 1f
                    true
                }
                DragEvent.ACTION_DROP -> {
                    v.alpha = 1f
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
