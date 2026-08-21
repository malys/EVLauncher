package com.evsuite.launcher.suite;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OnlineSuiteRepositoryTest {
    @Test public void allowsOnlyHttpsGithubReleaseHosts() {
        assertTrue(OnlineSuiteRepository.isAllowedUrl("https://api.github.com/repos/malys/example/releases/latest"));
        assertTrue(OnlineSuiteRepository.isAllowedUrl("https://release-assets.githubusercontent.com/file"));
        assertFalse(OnlineSuiteRepository.isAllowedUrl("http://github.com/file.apk"));
        assertFalse(OnlineSuiteRepository.isAllowedUrl("https://github.com.attacker.example/file.apk"));
        assertFalse(OnlineSuiteRepository.isAllowedUrl("file:///sdcard/file.apk"));
    }

    @Test public void acceptsTheStableAssetOfEverySuiteRelease() {
        assertTrue(OnlineSuiteRepository.isStableAsset("evprofile-stable-3.0.0.apk"));
        assertTrue(OnlineSuiteRepository.isStableAsset("evtasker-stable-2.1.0.apk"));
        assertFalse(OnlineSuiteRepository.isStableAsset("evprofile-unstable-3.0.0.1.apk"));
        assertFalse(OnlineSuiteRepository.isStableAsset("evprofile-3.0.0.apk"));
    }

    @Test public void readsVersionOnlyFromVersionedApkName() {
        assertEquals("1.4.12", OnlineSuiteRepository.versionFromAssetName("EVTasker-stable-1.4.12.apk"));
        assertNull(OnlineSuiteRepository.versionFromAssetName("EVTasker-stable.apk"));
        assertNull(OnlineSuiteRepository.versionFromAssetName("EVTasker-stable-1.4.12.zip"));
    }
}
