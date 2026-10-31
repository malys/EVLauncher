package com.evsuite.launcher

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.widget.EditText
import android.widget.Toast
import com.evsuite.hardware.diag.DiagnosticInbox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.concurrent.thread

/**
 * "Send to server" from the About dialog: this app's own log, posted to the driver's home
 * inbox ([DiagnosticInbox]) in the same format as EVTasker and EVChargePilot, so
 * `mise run inbox-pull` unpacks it with theirs.
 *
 * The log is logcat for this process only, stamped in seconds since boot: the head unit's wall
 * clock is the factory default for the first minutes of a drive, and boot-relative time is
 * what says how long the app took to come up.
 */
object DiagnosticSender {

    private const val PREFS = "diagnostic_inbox"
    private const val KEY_URL = "url"
    private const val MAX_LOG_LINES = 5000
    private const val REDACTED = "<redacted>"

    /** The address is shown every time: this dialog is also the confirmation that the log leaves the car. */
    @JvmStatic
    fun ask(activity: Activity, app: String, secrets: List<String?>) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val input = EditText(activity).apply {
            setSingleLine()
            hint = activity.getString(R.string.diag_send_hint)
            setText(prefs.getString(KEY_URL, ""))
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.diag_send)
            .setMessage(R.string.diag_send_message)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.diag_send_confirm) { _, _ ->
                val url = input.text.toString().trim()
                val refused = DiagnosticInbox.refuse(url)
                if (refused != null) {
                    Toast.makeText(activity, activity.getString(R.string.diag_send_refused, refused), Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString(KEY_URL, url).apply()
                    send(activity.applicationContext, app, url, secrets)
                }
            }
            .show()
    }

    private fun send(context: Context, app: String, url: String, secrets: List<String?>) {
        thread(name = "diagnostic-send") {
            val version = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "?"
            val content = report(
                header = "$app $version — ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}\n" +
                    "process started at boot + ${Process.getStartElapsedRealtime() / 1000} s, " +
                    "now boot + ${SystemClock.elapsedRealtime() / 1000} s",
                log = ownLogcat(),
                secrets = secrets,
            )
            val outcome = DiagnosticInbox.send(
                url,
                DiagnosticInbox.Artifact(
                    app = app,
                    version = version,
                    name = "$app-log-${System.currentTimeMillis()}.txt",
                    createdAtMs = System.currentTimeMillis(),
                    content = content.toByteArray(Charsets.UTF_8),
                ),
            )
            val message = when (outcome) {
                is DiagnosticInbox.Outcome.Sent -> context.getString(R.string.diag_send_ok)
                is DiagnosticInbox.Outcome.Failed -> context.getString(R.string.diag_send_failed, outcome.reason)
            }
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        }
    }

    /** An app may read its own log without READ_LOGS; `--pid` keeps a shared uid's neighbours out. */
    private fun ownLogcat(): String = runCatching {
        val proc = ProcessBuilder(
            "logcat", "-d", "-v", "threadtime", "-v", "monotonic",
            "--pid=${Process.myPid()}", "-t", MAX_LOG_LINES.toString()
        ).redirectErrorStream(true).start()
        proc.inputStream.bufferedReader().use { it.readText() }
    }.getOrElse { "logcat unavailable: ${it.javaClass.simpleName}: ${it.message}" }

    /**
     * The artifact text, with every [secrets] value and any `token=` / `api_key=` value masked —
     * an exception message can carry a request URL — and cut to what the inbox accepts, keeping
     * the most recent lines.
     */
    @JvmStatic
    fun report(header: String, log: String, secrets: List<String?>): String {
        var text = "$header\n\n$log"
        secrets.filterNotNull().filter { it.length >= 4 }.forEach { text = text.replace(it, REDACTED) }
        text = Regex("(token|api_key)=[^&\\s\"]+").replace(text, "$1=$REDACTED")
        // In chars, and the inbox caps UTF-8 bytes: half leaves room for any multi-byte text.
        val budget = DiagnosticInbox.MAX_CONTENT_BYTES / 2
        return if (text.length <= budget) text else header + "\n\n…\n" + text.takeLast(budget)
    }
}
