package com.evsuite.launcher.suite;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Dealer/Service mode: disables (or re-enables) the other EVSuite apps in one step so a
 * workshop tool sees no automation. The system's own enabled-state is the only record, so the
 * mode survives a reboot and cannot drift from what is really installed.
 *
 * Changing another package's enabled state needs CHANGE_COMPONENT_ENABLED_STATE, a signature
 * permission: on a build that does not hold it every call is refused with a SecurityException,
 * which is reported to the driver rather than swallowed.
 */
final class ServiceMode {
    private static final String TAG = "ServiceMode";
    private static final String LAUNCHER_PREFIX = "com.evsuite.launcher";

    private ServiceMode() {}

    /** Outcome of one toggle: how many apps changed, and how many the system refused. */
    static final class Result {
        final int changed;
        final int refused;
        Result(int changed, int refused) { this.changed = changed; this.refused = refused; }
    }

    /**
     * The launcher is the only way back, so it — stable or unstable id — is never a target.
     * Pure, so the "never disable myself" rule is covered by a JVM test.
     */
    static List<String> targets(List<String> catalogue, String self) {
        List<String> out = new ArrayList<>();
        for (String pkg : catalogue) {
            if (!pkg.startsWith(LAUNCHER_PREFIX) && !pkg.equals(self)) out.add(pkg);
        }
        return out;
    }

    static boolean isDisabled(PackageManager pm, String pkg) {
        try {
            int s = pm.getApplicationEnabledSetting(pkg);
            return s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        } catch (IllegalArgumentException notInstalled) {
            return false;
        }
    }

    static Result apply(Context context, boolean disable) {
        PackageManager pm = context.getPackageManager();
        List<String> catalogue = new ArrayList<>();
        for (SuiteAppState app : SuiteCatalog.apps()) catalogue.add(app.packageName);
        int changed = 0, refused = 0;
        for (String pkg : targets(catalogue, context.getPackageName())) {
            try {
                pm.getPackageInfo(pkg, 0); // not installed: nothing to do, not a refusal
                pm.setApplicationEnabledSetting(pkg, disable
                        ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                        : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);
                changed++;
            } catch (PackageManager.NameNotFoundException ignored) {
            } catch (RuntimeException e) { // SecurityException when the permission is not held
                Log.w(TAG, "Cannot change " + pkg, e);
                refused++;
            }
        }
        return new Result(changed, refused);
    }

    /** True when at least one installed target is currently disabled. */
    static boolean isActive(Context context) {
        PackageManager pm = context.getPackageManager();
        for (SuiteAppState app : SuiteCatalog.apps()) {
            if (!app.packageName.startsWith(LAUNCHER_PREFIX) && isDisabled(pm, app.packageName)) return true;
        }
        return false;
    }
}
