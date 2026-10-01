package com.evsuite.launcher.suite;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ServiceModeTest {
    private static final List<String> ALL = Arrays.asList("com.evsuite.profile",
            "com.evsuite.tasker", "com.evsuite.launcher", "com.evsuite.launcher.unstable");

    @Test public void neverTargetsTheLauncherItself() {
        List<String> t = ServiceMode.targets(ALL, "com.evsuite.launcher.unstable");
        assertFalse(t.contains("com.evsuite.launcher"));
        assertFalse(t.contains("com.evsuite.launcher.unstable"));
        assertEquals(Arrays.asList("com.evsuite.profile", "com.evsuite.tasker"), t);
    }

    @Test public void everyShippedAppExceptTheLauncherIsATarget() {
        java.util.List<String> cat = new java.util.ArrayList<>();
        for (SuiteAppState a : SuiteCatalog.apps()) cat.add(a.packageName);
        assertEquals(cat.size() - 1, ServiceMode.targets(cat, "x").size());
    }
}
