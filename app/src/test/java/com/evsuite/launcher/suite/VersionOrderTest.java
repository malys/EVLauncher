package com.evsuite.launcher.suite;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VersionOrderTest {
    @Test public void onlyHigherVersionCodeIsUpgrade() {
        assertTrue(VersionOrder.isUpgrade(11, 10));
        assertFalse(VersionOrder.isUpgrade(10, 10));
        assertFalse(VersionOrder.isUpgrade(9, 10));
    }

    @Test public void releaseVersionsAreComparedNumerically() {
        assertTrue(VersionOrder.isNewer("v1.10.0", "1.9.9"));
        assertFalse(VersionOrder.isNewer("1.4.0", "1.4"));
        assertFalse(VersionOrder.isNewer("1.3.9", "1.4.0"));
    }
}
