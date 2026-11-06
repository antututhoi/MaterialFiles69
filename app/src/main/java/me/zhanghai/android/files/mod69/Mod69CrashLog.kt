package me.zhanghai.android.files.mod69

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.zhanghai.android.files.app.application
import java.io.File

/**
 * 69 mod: when the app crashes, the error is saved; next time the app opens it is shown with a
 * Copy button, so it can be sent to fix the crash.
 */
object Mod69CrashLog {
    private fun file(context: Context) = File(context.filesDir, "mod69_crash.txt")

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                file(application).writeText(
                    "69-Material Files crash\n" + java.util.Date() + "\nthread: " + thread.name + "\n\n" +
                        android.util.Log.getStackTraceString(error)
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun showIfAny(activity: Activity) {
        val f = file(activity)
        if (!f.exists()) return
        val text = runCatching { f.readText() }.getOrDefault("")
        f.delete()
        if (text.isBlank()) return
        MaterialAlertDialogBuilder(activity)
            .setTitle("The app crashed last time")
            .setMessage(text.take(4000))
            .setPositiveButton("Copy") { _, _ ->
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", text))
                Toast.makeText(activity, "Copied – send it to get it fixed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
