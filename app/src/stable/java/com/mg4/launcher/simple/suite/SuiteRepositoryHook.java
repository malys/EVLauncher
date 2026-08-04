package com.mg4.launcher.simple.suite;

import android.content.Context;

import java.io.File;
import java.util.List;

/** Stable stays offline: it reports installed suite apps and performs no remote action. */
public final class SuiteRepositoryHook {
    private SuiteRepositoryHook() {}

    public static boolean isOnline() { return false; }
    public static List<SuiteAppState> inspect(Context context) { return SuiteCatalog.installed(context); }
    public static File download(Context context, SuiteAppState app) { return null; }
    public static boolean install(Context context, SuiteAppState app, File apk) { return false; }
}
