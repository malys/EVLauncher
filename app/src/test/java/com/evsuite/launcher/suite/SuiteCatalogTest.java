package com.evsuite.launcher.suite;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    /** Only MG4Link escapes the suite key, and only for its own pinned certificate. */
    @Test public void onlyMg4LinkIsThirdParty() {
        for (SuiteAppState app : SuiteCatalog.apps()) {
            if (app.packageName.equals("com.allgo.app.androidauto")) {
                assertEquals(SuiteCatalog.MG4LINK_SIGNER, app.thirdPartySigner);
            } else {
                assertNull(app.packageName, app.thirdPartySigner);
            }
        }
    }

    private static List<String> packages() {
        List<SuiteAppState> apps = SuiteCatalog.apps();
        List<String> names = new java.util.ArrayList<>();
        for (SuiteAppState app : apps) names.add(app.packageName);
        return names;
    }
}
