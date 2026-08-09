package com.mg4.launcher.simple.suite;

import android.content.Context;

import java.io.File;
import java.util.List;

/** Flavor seam: only unstable contains the network-backed MG4Suite catalogue. */
public final class SuiteRepository {
    private SuiteRepository() {}

    public static List<SuiteAppState> inspect(Context context) {
        return OnlineSuiteRepository.inspect(context);
    }
    public static File download(Context context, SuiteAppState app) {
        return OnlineSuiteRepository.download(context, app);
    }
    public static void purgeCachedApks(Context context) { OnlineSuiteRepository.purgeCachedApks(context); }
}
