package me.zhanghai.android.files.mod69

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.commit
import java8.nio.file.Paths
import me.zhanghai.android.files.app.AppActivity
import me.zhanghai.android.files.filelist.FileListActivity
import me.zhanghai.android.files.filelist.FileListFragment
import me.zhanghai.android.files.util.putArgs

/**
 * 69 mod: two file lists at once – side by side when the screen is wide, one above the other when tall.
 * Copy / cut in one panel and paste in the other (the clipboard is shared).
 */
class Mod69DualActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(android.R.id.content)
        val wide = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val root = LinearLayout(this).apply {
            orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        for (i in 0..1) {
            val frame = FrameLayout(this).apply { id = ID_BASE + i }
            val lp = if (wide) {
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            } else {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            root.addView(frame, lp)
            if (i == 0) {
                root.addView(
                    View(this).apply { setBackgroundColor(0x55888888) },
                    if (wide) LinearLayout.LayoutParams(2, ViewGroup.LayoutParams.MATCH_PARENT)
                    else LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2),
                )
            }
        }
        setContentView(root)
        if (savedInstanceState == null) {
            val start = intent.getStringArrayExtra(EXTRA_PATHS)
            val home = Environment.getExternalStorageDirectory().absolutePath
            supportFragmentManager.commit {
                for (i in 0..1) {
                    val path = start?.getOrNull(i) ?: Mod69Recent.liveData.value?.getOrNull(i) ?: home
                    val viewIntent = FileListActivity.createViewIntent(Paths.get(path))
                    add(ID_BASE + i, FileListFragment().putArgs(FileListFragment.Args(viewIntent)), "panel$i")
                }
            }
        }
    }

    companion object {
        private const val ID_BASE = 0x6900
        const val EXTRA_PATHS = "mod69_paths"

        fun intent(context: android.content.Context): Intent = Intent(context, Mod69DualActivity::class.java)
    }
}
