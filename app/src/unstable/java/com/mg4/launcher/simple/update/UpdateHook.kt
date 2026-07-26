package com.mg4.launcher.simple.update

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.mg4.launcher.simple.R
import java.io.File

/**
 * Unstable channel: checks GitHub pre-releases and downloads a newer unstable APK.
 *
 * The install itself stays manual — the user taps the downloaded file. The launcher is not
 * privileged enough to install silently, and asking for REQUEST_INSTALL_PACKAGES to save one
 * tap on a test channel is not a trade worth making.
 */
object UpdateHook {

    private const val TAG = "UpdateHook"

    fun isSupported(): Boolean = true

    /** Fire-and-forget check. Network work runs off the main thread. */
    @JvmStatic
    @JvmOverloads
    fun checkInBackground(context: Context, userInitiated: Boolean = false) {
        val app = context.applicationContext
        Thread({
            try {
                val current = app.packageManager.getPackageInfo(app.packageName, 0).versionName
                    ?: return@Thread
                val update = OtaUpdater.check(current)
                if (update == null) {
                    Log.i(TAG, "No newer unstable than $current")
                    if (userInitiated) toast(app, R.string.update_up_to_date)
                    return@Thread
                }

                // Anything already downloaded is verified before the user is pointed at it:
                // a file in public Downloads can be swapped by another app.
                val existing = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    OtaUpdater.downloadFileName(update.versionName)
                )
                if (existing.isFile) {
                    if (!OtaUpdater.signatureMatchesRunningApp(app, existing)) {
                        val deleted = existing.delete()
                        Log.w(TAG, "Rejected a foreign-signed update (deleted=$deleted)")
                        if (userInitiated) toast(app, R.string.update_rejected)
                    } else {
                        Log.i(TAG, "Update already downloaded and verified: ${existing.name}")
                        if (userInitiated) toast(app, R.string.update_already_downloaded)
                    }
                    return@Thread
                }

                OtaUpdater.download(app, update)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        app,
                        app.getString(R.string.update_downloading, update.versionName),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
                if (userInitiated) toast(app, R.string.update_check_failed)
            }
        }, "ota-check").start()
    }

    private fun toast(context: Context, resId: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
        }
    }
}
