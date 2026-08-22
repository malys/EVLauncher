package com.evsuite.launcher.suite;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import java.io.File;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/** Sharing the suite signing key is the trust boundary for every downloaded APK. */
final class SuiteApkSecurity {
    private static final String TAG = "SuiteRepository";
    /** GET_SIGNATURES rides along: some head units leave signingInfo empty on an archive. */
    private static final int FLAGS =
            PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES;

    private SuiteApkSecurity() {}

    /** A refusal carries the short fingerprints, so a driver can report what did not match. */
    static final class Verdict {
        final boolean trusted;
        final String detail;

        private Verdict(boolean trusted, String detail) {
            this.trusted = trusted;
            this.detail = detail;
        }
    }

    static Verdict inspect(Context context, File apk, String expectedPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo archive = pm.getPackageArchiveInfo(apk.getAbsolutePath(), FLAGS);
            if (archive == null) return new Verdict(false, "unreadable APK");
            if (!expectedPackage.equals(archive.packageName)) {
                return new Verdict(false, "package " + archive.packageName);
            }
            Set<String> downloaded = fingerprints(archive);
            Set<String> accepted = fingerprints(pm.getPackageInfo(context.getPackageName(), FLAGS));
            // Android accepts an update signed by any certificate the installed app knows,
            // current or rotated away; sharing none of them means a different key.
            if (!downloaded.isEmpty() && !Collections.disjoint(downloaded, accepted)) {
                return new Verdict(true, null);
            }
            Log.w(TAG, "Signature mismatch for " + expectedPackage
                    + ": APK " + downloaded + " vs installed " + accepted);
            return new Verdict(false, shortest(downloaded) + " ≠ " + shortest(accepted));
        } catch (Exception e) {
            Log.w(TAG, "Signature check failed for " + expectedPackage, e);
            return new Verdict(false, e.getClass().getSimpleName());
        }
    }

    /** Every certificate the package is signed with, plus the ones it was signed with before. */
    private static Set<String> fingerprints(PackageInfo info) throws Exception {
        Set<String> result = new HashSet<>();
        if (info == null) return result;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        if (info.signingInfo != null) {
            add(result, digest, info.signingInfo.getApkContentsSigners());
            if (!info.signingInfo.hasMultipleSigners()) {
                add(result, digest, info.signingInfo.getSigningCertificateHistory());
            }
        }
        add(result, digest, info.signatures);
        return result;
    }

    private static void add(Set<String> result, MessageDigest digest, Signature[] signatures) {
        if (signatures == null) return;
        for (Signature signature : signatures) {
            if (signature == null) continue;
            byte[] bytes = digest.digest(signature.toByteArray());
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) hex.append(String.format("%02x", value & 0xff));
            result.add(hex.toString());
        }
    }

    private static String shortest(Set<String> fingerprints) {
        if (fingerprints.isEmpty()) return "none";
        return new TreeSet<>(fingerprints).first().substring(0, 8);
    }
}
