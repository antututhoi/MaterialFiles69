package me.zhanghai.android.files.mod69

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View

/**
 * 69 mod: MiXplorer-style "Startup lock". When it is on, the app asks for the phone's own screen lock
 * (PIN / pattern / password / fingerprint) each time it is started fresh. Nothing is shown until it is unlocked.
 * Not asked while another app is choosing a file through us (that would break the chooser).
 */
object Mod69Lock {
    @Volatile private var unlocked = false
    val isUnlocked: Boolean get() = unlocked

    fun guard(activity: Activity) {
        if (!Mod69Look.startupLock || unlocked) return
        val action = activity.intent?.action
        if (action != null && action != Intent.ACTION_MAIN && action != Intent.ACTION_VIEW) return
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isDeviceSecure) {   // the phone has no screen lock: nothing to ask with
            unlocked = true
            return
        }
        activity.findViewById<View>(android.R.id.content)?.visibility = View.INVISIBLE
        activity.startActivity(Intent(activity, Mod69LockActivity::class.java))
    }

    fun onUnlocked() { unlocked = true }
}

/** Shows the system "confirm your screen lock" screen; closes the whole app when it is cancelled. */
class Mod69LockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        @Suppress("DEPRECATION")
        val intent = km.createConfirmDeviceCredentialIntent("69-Material Files", "Unlock to see your files")
        if (intent == null) {
            Mod69Lock.onUnlocked()
            finish()
        } else {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 69)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            Mod69Lock.onUnlocked()
            finish()
        } else {
            finishAffinity()
        }
    }
}
