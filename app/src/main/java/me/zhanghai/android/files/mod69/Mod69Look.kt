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
    val showExtensions: Boolean get() = prefs.getBoolean("mod69_show_extensions", true)
    val fullNames: Boolean get() = prefs.getBoolean("mod69_full_names", false)
    val highlightVisited: Boolean get() = prefs.getBoolean("mod69_highlight_visited", true)
    val cropThumbnails: Boolean get() = prefs.getBoolean("mod69_crop_thumbnails", true)
    val storageBars: Boolean get() = prefs.getBoolean("mod69_storage_bars", true)
    val drawerFromBottom: Boolean get() = prefs.getBoolean("mod69_drawer_bottom", false)

    /** Folders opened in this session (and the recent ones), for "Highlight visited folders". */
    val visited: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet<String>())

    /** Accent colour, pure black and monospace font (MiXplorer look); call right after the theme is set. */
    fun applyTheme(activity: Activity) {
        val theme = activity.theme
        val accent = prefs.getString("mod69_accent", "none") ?: "none"
        if (accent != "none") {
            val id = when (accent) {
                "green" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_Green
                "cyan" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_Cyan
                "blue" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_Blue
                "purple" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_Purple
                "pink" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_Pink
                "red" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_Red
                "orange" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_Orange
                "yellow" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_Yellow
                "white" -> me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Accent_White
                else -> 0
            }
            if (id != 0) theme.applyStyle(id, true)
        }
        val night = (activity.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        if (night && prefs.getBoolean("mod69_amoled", false)) {
            theme.applyStyle(me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Black, true)
        }
        if (prefs.getBoolean("mod69_mono_font", false)) {
            theme.applyStyle(me.zhanghai.android.files.R.style.ThemeOverlay_Mod69_Mono, true)
        }
    }

    /** Extra grid columns (pinch to zoom). */
    var gridDelta: Int
        get() = prefs.getInt("mod69_grid_delta", 0)
        set(v) {
            prefs.edit().putInt("mod69_grid_delta", v.coerceIn(-3, 6)).apply()
        }

    /** Pinch out = bigger thumbnails (fewer columns), pinch in = more columns. [apply] returns false when not a grid. */
    fun pinchToZoom(list: RecyclerView, apply: () -> Boolean) {
        if (!prefs.getBoolean("mod69_pinch_zoom", true)) return
        var acc = 1f
        val detector = android.view.ScaleGestureDetector(list.context, object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: android.view.ScaleGestureDetector): Boolean {
                acc = 1f
                return true
            }

            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                acc *= detector.scaleFactor
                val step = when {
                    acc > 1.35f -> -1
                    acc < 0.75f -> 1
                    else -> 0
                }
                if (step != 0) {
                    val old = gridDelta
                    gridDelta = old + step
                    if (!apply()) gridDelta = old
                    acc = 1f
                }
                return true
            }
        })
        list.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                detector.onTouchEvent(e)
                return e.pointerCount > 1 && detector.isInProgress
            }

            override fun onTouchEvent(rv: RecyclerView, e: android.view.MotionEvent) {
                detector.onTouchEvent(e)
            }
        })
    }

    /** Name as shown in the list. */
    fun displayName(name: String, isDirectory: Boolean): String {
        if (showExtensions || isDirectory) return name
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

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
