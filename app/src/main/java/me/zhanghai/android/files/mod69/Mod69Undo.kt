package me.zhanghai.android.files.mod69

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java8.nio.file.Path
import java8.nio.file.Paths
import java.io.File
import me.zhanghai.android.files.filejob.FileJobService

/**
 * 69 mod (MiXplorer main menu "Undo"): remembers the last renames, moves and recycle-bin deletes
 * of this session and undoes the newest one. Copies are not undone (nothing was lost).
 * Only files on the phone; an item that is no longer where it was put is skipped.
 */
object Mod69Undo {
    private sealed class Op(val label: String) {
        class Rename(val newPath: String, val oldName: String, label: String) : Op(label)
        class Move(val items: List<Pair<String, String>>, label: String) : Op(label) // new path -> old folder
        class Bin(val inBin: List<String>, label: String) : Op(label)
    }

    private val ops = ArrayDeque<Op>()

    private fun push(op: Op) {
        synchronized(ops) {
            ops.addLast(op)
            while (ops.size > 30) ops.removeFirst()
        }
    }

    private fun local(p: Path) = p.toString().startsWith("/")

    fun rename(path: Path, newName: String) {
        if (!local(path)) return
        val parent = path.parent?.toString() ?: return
        val old = path.fileName.toString()
        if (old == newName) return
        push(Op.Rename("$parent/$newName", old, "Rename \"$newName\" back to \"$old\""))
    }

    fun move(sources: List<Path>, target: Path) {
        if (!local(target) || sources.any { !local(it) }) return
        val items = sources.mapNotNull { s ->
            val oldDir = s.parent?.toString() ?: return@mapNotNull null
            if (oldDir == target.toString()) null else "$target/${s.fileName}" to oldDir
        }
        if (items.isEmpty()) return
        val what = if (items.size == 1) "\"${File(items[0].first).name}\"" else "${items.size} items"
        push(Op.Move(items, "Move $what back to ${File(items[0].second).name}"))
    }

    fun bin(binDir: File, sources: List<Path>) {
        val inBin = sources.map { File(binDir, it.fileName.toString()).absolutePath }
        val what = if (inBin.size == 1) "\"${File(inBin[0]).name}\"" else "${inBin.size} items"
        push(Op.Bin(inBin, "Bring back $what from the recycle bin"))
    }

    fun show(context: Context, refresh: () -> Unit) {
        val op = synchronized(ops) { ops.lastOrNull() }
        if (op == null) {
            Toast.makeText(context, "Nothing to undo", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(context)
            .setTitle("Undo")
            .setMessage(op.label + "?")
            .setPositiveButton("Undo") { _, _ ->
                synchronized(ops) { ops.remove(op) }
                val n = undo(op, context)
                Toast.makeText(
                    context, if (n == 0) "Nothing could be undone (the items were changed again)" else "Undone",
                    Toast.LENGTH_SHORT,
                ).show()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun undo(op: Op, context: Context): Int = when (op) {
        is Op.Rename -> {
            val f = File(op.newPath)
            if (f.exists() && !File(f.parentFile, op.oldName).exists()) {
                FileJobService.rename(Paths.get(op.newPath), op.oldName, context); 1
            } else {
                0
            }
        }
        is Op.Move -> {
            var n = 0
            op.items.filter { File(it.first).exists() && !File(it.second, File(it.first).name).exists() }
                .groupBy { it.second }
                .forEach { (dir, list) ->
                    File(dir).mkdirs()
                    FileJobService.move(list.map { Paths.get(it.first) }, Paths.get(dir), context)
                    n += list.size
                }
            n
        }
        is Op.Bin -> Mod69Bin.restore(op.inBin.filter { File(it).exists() }.map { Paths.get(it) }, context)
    }
}
