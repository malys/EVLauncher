package com.mg4.launcher.simple.update

import android.content.Context

/**
 * Stable channel: no self-update, by construction.
 *
 * This is not a disabled feature — [OtaUpdater] is not in the APK at all. A stable build
 * cannot be made to fetch and install code by flipping a preference, there is no update URL
 * in it to attack, and the stable manifest carries no INTERNET permission.
 */
object UpdateHook {
    /** Does nothing. Stable users install updates themselves, offline, from a USB stick. */
    @JvmStatic
    @JvmOverloads
    fun checkInBackground(context: Context, userInitiated: Boolean = false) {}

    fun isSupported(): Boolean = false
}
