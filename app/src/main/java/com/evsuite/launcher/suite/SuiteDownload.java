package com.evsuite.launcher.suite;

import java.io.File;

import com.evsuite.launcher.R;

/** Outcome of one APK fetch: the verified file, or the reason the head unit did not get one. */
public final class SuiteDownload {
    /** Every failure carries its own message: "download failed" alone hides the cause. */
    public enum Failure {
        BLOCKED_URL(R.string.suite_download_error_blocked),
        STORAGE(R.string.suite_download_error_storage),
        SERVER(R.string.suite_download_error_server),
        NETWORK(R.string.suite_download_error_network),
        TOO_LARGE(R.string.suite_download_error_too_large),
        REJECTED(R.string.suite_apk_invalid);

        public final int messageId;

        Failure(int messageId) { this.messageId = messageId; }
    }

    public final File file;
    public final Failure failure;
    /** HTTP status or exception name; appended to the message so a report carries the cause. */
    public final String detail;

    private SuiteDownload(File file, Failure failure, String detail) {
        this.file = file;
        this.failure = failure;
        this.detail = detail;
    }

    static SuiteDownload of(File file) { return new SuiteDownload(file, null, null); }
    static SuiteDownload failed(Failure failure) { return new SuiteDownload(null, failure, null); }
    static SuiteDownload failed(Failure failure, String detail) {
        return new SuiteDownload(null, failure, detail);
    }
}
