package com.mg4.launcher.simple.suite;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class OnlineSuiteRepositoryTest {
    @Test public void acceptsOnlyHttpsReleaseHosts() {
        assertTrue(OnlineSuiteRepository.isAllowedUrl("https://api.github.com/repos/malys/MG4Tasker/releases/latest"));
        assertTrue(OnlineSuiteRepository.isAllowedUrl("https://release-assets.githubusercontent.com/file.apk"));
        assertFalse(OnlineSuiteRepository.isAllowedUrl("http://github.com/file.apk"));
        assertFalse(OnlineSuiteRepository.isAllowedUrl("https://github.com.attacker.invalid/file.apk"));
        assertFalse(OnlineSuiteRepository.isAllowedUrl("file:///sdcard/file.apk"));
    }

    @Test public void rollingAssetCarriesComparableVersion() {
        assertEquals("1.0.0.25", OnlineSuiteRepository.versionFromAssetName(
                "MG4Tasker-unstable-1.0.0.25.apk"));
        assertEquals("1.4.1.5", OnlineSuiteRepository.versionFromAssetName(
                "MG4SwipeLauncher-unstable-1.4.1.5.apk"));
    }
}
