package com.mg4.launcher.simple.suite;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/** Fixed allowlist: an offline manifest cannot introduce an arbitrary package. */
final class SuiteCatalog {
    private SuiteCatalog() {}

    static List<SuiteAppState> apps() {
        List<SuiteAppState> apps = new ArrayList<>();
        apps.add(new SuiteAppState("MG4 Control · offline", "com.mg4.control.offline"));
        apps.add(new SuiteAppState("MG4 Tasker", "com.mg4.tasker"));
        apps.add(new SuiteAppState("MG4 ABRP Uploader", "com.mg4.abrptelemetry"));
        apps.add(new SuiteAppState("MG4 Swipe Launcher", "com.mg4.launcher.swipe"));
        apps.add(new SuiteAppState("MG4 Simple Launcher", "com.mg4.launcher.simple"));
        return apps;
    }

    static List<SuiteAppState> installed(Context context) {
        return readInstalled(context, apps());
    }

    private static List<SuiteAppState> readInstalled(Context context, List<SuiteAppState> apps) {
        for (SuiteAppState app : apps) {
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(app.packageName, 0);
                app.installedVersion = info.versionName;
                app.installedVersionCode = info.getLongVersionCode();
                app.action = SuiteAppState.Action.OPEN;
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        return apps;
    }
}
