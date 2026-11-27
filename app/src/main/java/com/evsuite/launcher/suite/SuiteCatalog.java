package com.evsuite.launcher.suite;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/** Fixed allowlist: an offline manifest cannot introduce an arbitrary package. */
final class SuiteCatalog {
    private SuiteCatalog() {}

    static final String MG4LINK_SIGNER =
            "c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8";

    static List<SuiteAppState> apps() {
        List<SuiteAppState> apps = new ArrayList<>();
        apps.add(new SuiteAppState("EVProfile", "com.evsuite.profile"));
        apps.add(new SuiteAppState("EVTasker", "com.evsuite.tasker"));
        apps.add(new SuiteAppState("EVABRPUploader", "com.evsuite.abrp"));
        apps.add(new SuiteAppState("EVSwipe", "com.evsuite.swipe"));
        apps.add(new SuiteAppState("EVChargePilot", "com.evsuite.chargepilot"));
        apps.add(new SuiteAppState("EVLauncher", "com.evsuite.launcher"));
        // Third party (Skittle6938/MG4Link), signed with the public AOSP test key: the pin only
        // stops a switch to another key, it does not prove who built the APK. Owner choice
        // 2026-11-27, shown on the entry as a warning.
        apps.add(new SuiteAppState("MG4Link", "com.allgo.app.androidauto", MG4LINK_SIGNER));
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
                app.disabled = ServiceMode.isDisabled(context.getPackageManager(), app.packageName);
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        return apps;
    }
}
