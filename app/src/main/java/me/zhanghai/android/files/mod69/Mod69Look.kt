package me.zhanghai.android.files.mod69

import android.app.Activity
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.files.app.application

/** 69 mod: "No animations" and "Solid navigation bar". */
object Mod69Look {
    private val prefs get() = PreferenceManager.getDefaultSharedPreferences(application)

    val noAnimations: Boolean get() = prefs.getBoolean("mod69_no_animations", false)

    /** Lists: no item animations. */
    fun apply(list: RecyclerView) {
        if (noAnimations) {
            list.itemAnimator = null
            list.layoutAnimation = null
        }
    }

    /** Screens open / close without sliding. */
    @Suppress("DEPRECATION")
    fun noTransition(activity: Activity, opening: Boolean) {
        if (!noAnimations) return
        if (Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(
                if (opening) Activity.OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0
            )
        } else {
            activity.overridePendingTransition(0, 0)
        }
    }

    /**
     * A bar in the app's background colour behind the phone's navigation buttons
     * (edge-to-edge apps are see-through there).
     */
    fun solidNavBar(activity: Activity) {
        if (!prefs.getBoolean("mod69_solid_nav_bar", true)) return
        val decor = activity.window.decorView as? ViewGroup ?: return
        if (decor.findViewWithTag<View>(TAG) != null) return
        val tv = TypedValue()
        // not for see-through dialog screens
        if (activity.theme.resolveAttribute(android.R.attr.windowIsTranslucent, tv, true) && tv.data != 0) return
        val color = if (activity.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, tv, true)) {
            tv.data
        } else {
            0xFF000000.toInt()
        }
        val bar = View(activity).apply {
            tag = TAG
            setBackgroundColor(color)
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        decor.addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.BOTTOM))
        ViewCompat.setOnApplyWindowInsetsListener(bar) { v, insets ->
            val h = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = v.layoutParams
            if (lp.height != h) {
                lp.height = h
                v.layoutParams = lp
            }
            insets
        }
        ViewCompat.requestApplyInsets(bar)
    }

    private const val TAG = "mod69_nav_bar"
}
