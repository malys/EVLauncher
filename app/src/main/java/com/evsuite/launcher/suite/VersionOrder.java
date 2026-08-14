package com.evsuite.launcher.suite;

final class VersionOrder {
    private VersionOrder() {}

    static boolean isUpgrade(long candidateCode, long installedCode) {
        return candidateCode > installedCode;
    }

    static boolean isNewer(String candidate, String installed) {
        int[] left = segments(candidate);
        int[] right = segments(installed);
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int l = i < left.length ? left[i] : 0;
            int r = i < right.length ? right[i] : 0;
            if (l != r) return l > r;
        }
        return false;
    }

    private static int[] segments(String version) {
        if (version == null) return new int[0];
        String core = version.replaceFirst("^[vV]", "").split("[-+]", 2)[0];
        String[] parts = core.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String digits = parts[i].replaceFirst("^(\\d*).*$", "$1");
            try { result[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits); }
            catch (NumberFormatException ignored) { result[i] = 0; }
        }
        return result;
    }
}
