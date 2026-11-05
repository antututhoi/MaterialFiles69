package me.zhanghai.android.files.mod69

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java8.nio.file.Path
import me.zhanghai.android.files.filejob.FileJobService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 69 mod: rename many files with a pattern.
 * {name} old name without extension, {ext} extension, {n} number, {n:3} number with 3 digits, {date} yyyy-MM-dd.
 * Optional find / replace runs on the old name first.
 */
object Mod69BatchRename {

    fun newName(old: String, pattern: String, index: Int, start: Int, find: String, replace: String): String {
        val dot = old.lastIndexOf('.')
        var base = if (dot > 0) old.substring(0, dot) else old
        val ext = if (dot > 0) old.substring(dot + 1) else ""
        if (find.isNotEmpty()) base = base.replace(find, replace)
        val num = start + index
        var out = pattern.ifBlank { "{name}" }
            .replace(Regex("\\{n:(\\d+)\\}")) { m -> num.toString().padStart(m.groupValues[1].toInt(), '0') }
            .replace("{n}", num.toString())
            .replace("{name}", base)
            .replace("{ext}", ext)
            .replace("{date}", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        if (!pattern.contains("{ext}") && ext.isNotEmpty()) out = "$out.$ext"
        return out.replace('/', '_')
    }

    fun show(context: Context, paths: List<Path>, onDone: () -> Unit) {
        if (paths.isEmpty()) return
        val pad = (16 * context.resources.displayMetrics.density).toInt()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        fun field(hint: String, value: String) = EditText(context).apply {
            this.hint = hint
            setText(value)
            layout.addView(this)
        }
        val pattern = field("Pattern: {name} {ext} {n} {n:3} {date}", "{name}_{n:3}")
        val start = field("Start number", "1").apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val find = field("Find (optional)", "")
        val replace = field("Replace with", "")
        val preview = TextView(context).apply { setPadding(0, pad / 2, 0, 0) }
        layout.addView(preview)
        val sorted = paths.sortedBy { it.fileName.toString().lowercase() }
        fun names(): List<String> = sorted.mapIndexed { i, p ->
            newName(
                p.fileName.toString(), pattern.text.toString(), i,
                start.text.toString().toIntOrNull() ?: 1, find.text.toString(), replace.text.toString(),
            )
        }
        fun update() {
            val n = names()
            preview.text = sorted.take(4).mapIndexed { i, p -> "${p.fileName} → ${n[i]}" }.joinToString("\n") +
                if (sorted.size > 4) "\n… (${sorted.size} files)" else ""
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = update()
        }
        listOf(pattern, start, find, replace).forEach { it.addTextChangedListener(watcher) }
        update()
        val dialog: AlertDialog = MaterialAlertDialogBuilder(context)
            .setTitle("Rename ${paths.size} files")
            .setView(layout)
            .setPositiveButton("Rename") { _, _ ->
                val n = names()
                if (n.toSet().size != n.size) {
                    android.widget.Toast.makeText(context, "Names would repeat – add {n}", android.widget.Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                sorted.forEachIndexed { i, p ->
                    if (p.fileName.toString() != n[i]) FileJobService.rename(p, n[i], context)
                }
                onDone()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()
    }
}
