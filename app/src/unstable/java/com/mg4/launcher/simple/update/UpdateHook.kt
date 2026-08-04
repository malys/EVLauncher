package com.mg4.launcher.simple.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.mg4.launcher.simple.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unstable channel: checks, downloads, verifies and installs a newer unstable APK.
 */
object UpdateHook {

    private const val TAG = "UpdateHook"
    private val running = AtomicBoolean(false)

    fun isSupported(): Boolean = true

    /** Fire-and-forget check. Network work runs off the main thread. */
    @JvmStatic
    @JvmOverloads
    fun checkInBackground(context: Context, userInitiated: Boolean = false) {
        if (!running.compareAndSet(false, true)) return
        val app = context.applicationContext
        Thread({
            try {
                OtaUpdater.purgeCachedApks(app)
                val current = app.packageManager.getPackageInfo(app.packageName, 0).versionName
                    ?: return@Thread
                val update = OtaUpdater.check(current)
                if (update == null) {
                    Log.i(TAG, "No newer unstable than $current")
                    if (userInitiated) toast(app, R.string.update_up_to_date)
                    return@Thread
                }

                val apk = OtaUpdater.download(app, update) ?: return@Thread
                val installed = try { OtaUpdater.install(app, apk) } finally { apk.delete() }
                if (!installed) {
                    Log.w(TAG, "Automatic update installation failed")
                    if (userInitiated) toast(app, R.string.update_check_failed)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
                if (userInitiated) toast(app, R.string.update_check_failed)
            } finally {
                running.set(false)
            }
        }, "ota-check").start()
    }

    private fun toast(context: Context, resId: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
        }
    }
}
