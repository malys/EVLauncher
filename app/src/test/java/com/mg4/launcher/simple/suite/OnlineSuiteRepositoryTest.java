package com.mg4.launcher.simple.suite;

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

    @Test public void readsVersionOnlyFromVersionedApkName() {
        assertEquals("1.4.12", OnlineSuiteRepository.versionFromAssetName("MG4Tasker-stable-1.4.12.apk"));
        assertNull(OnlineSuiteRepository.versionFromAssetName("MG4Tasker-stable.apk"));
        assertNull(OnlineSuiteRepository.versionFromAssetName("MG4Tasker-stable-1.4.12.zip"));
    }
}
