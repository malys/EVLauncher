package com.mg4.launcher.simple.suite;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.io.File;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/** Certificate equality is the trust boundary for every downloaded suite APK. */
final class SuiteApkSecurity {
    private SuiteApkSecurity() {}

    static boolean isTrustedSuiteApk(Context context, File apk, String expectedPackage) {
        PackageInfo info = context.getPackageManager().getPackageArchiveInfo(
                apk.getAbsolutePath(), 0);
        return info != null && expectedPackage.equals(info.packageName)
                && matchesSuiteCertificate(context, apk);
    }

    static boolean matchesSuiteCertificate(Context context, File apk) {
        PackageManager pm = context.getPackageManager();
        try {
            int flags = Build.VERSION.SDK_INT >= 28
                    ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
            PackageInfo archive = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
            PackageInfo running = pm.getPackageInfo(context.getPackageName(), flags);
            Set<String> archiveCerts = fingerprints(archive);
            Set<String> runningCerts = fingerprints(running);
            return !archiveCerts.isEmpty() && archiveCerts.equals(runningCerts);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Set<String> fingerprints(PackageInfo info) throws Exception {
        Set<String> result = new HashSet<>();
        if (info == null) return result;
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) {
            signatures = info.signingInfo.hasMultipleSigners()
                    ? info.signingInfo.getApkContentsSigners()
                    : info.signingInfo.getSigningCertificateHistory();
        } else {
            signatures = info.signatures;
        }
        if (signatures == null) return result;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (Signature signature : signatures) {
            byte[] bytes = digest.digest(signature.toByteArray());
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) hex.append(String.format("%02x", value & 0xff));
            result.add(hex.toString());
        }
        return result;
    }
}
