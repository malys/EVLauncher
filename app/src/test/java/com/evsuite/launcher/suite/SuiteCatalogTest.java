package com.evsuite.launcher.suite;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SuiteCatalogTest {
    /** The legacy `.offline` id made EVProfile undetectable: it is no longer published. */
    @Test public void listsTheStableEvProfileApplicationId() {
        assertTrue(packages().contains("com.evsuite.profile"));
    }

    @Test public void everyCatalogueEntryHasAReleaseEndpoint() {
        for (String packageName : packages()) {
            assertNotNull(packageName, OnlineSuiteRepository.releaseApi(packageName));
        }
    }

    private static List<String> packages() {
        List<SuiteAppState> apps = SuiteCatalog.apps();
        List<String> names = new java.util.ArrayList<>();
        for (SuiteAppState app : apps) names.add(app.packageName);
        return names;
    }
}
