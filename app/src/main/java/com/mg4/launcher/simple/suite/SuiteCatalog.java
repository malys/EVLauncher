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
        apps.add(new SuiteAppState("MG4 Control", "com.mg4.control"));
        apps.add(new SuiteAppState("MG4 Tasker", "com.mg4.tasker"));
        apps.add(new SuiteAppState("MG4 ABRP Uploader", "com.mg4.abrptelemetry"));
        apps.add(new SuiteAppState("MG4 Swipe Launcher", "com.mg4.launcher.swipe"));
        apps.add(new SuiteAppState("MG4 Simple Launcher", "com.mg4.launcher.simple"));
        return apps;
    }

    static List<SuiteAppState> unstableApps() {
        List<SuiteAppState> apps = new ArrayList<>();
        // MG4Control has no rolling unstable channel; its online flavor is the installable one.
        apps.add(new SuiteAppState("MG4 Control", "com.mg4.control"));
        apps.add(new SuiteAppState("MG4 Tasker · unstable", "com.mg4.tasker.unstable"));
        apps.add(new SuiteAppState("MG4 ABRP Uploader · unstable", "com.mg4.abrptelemetry.unstable"));
        apps.add(new SuiteAppState("MG4 Swipe Launcher · unstable", "com.mg4.launcher.swipe.unstable"));
        apps.add(new SuiteAppState("MG4 Simple Launcher · unstable", "com.mg4.launcher.simple.unstable"));
        return apps;
    }

    static List<SuiteAppState> installed(Context context) {
        return readInstalled(context, apps());
    }

    static List<SuiteAppState> installedUnstable(Context context) {
        return readInstalled(context, unstableApps());
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
