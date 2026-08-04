package com.mg4.launcher.simple.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The updater decides what code gets downloaded onto a head unit. Its two gates — host
 * allowlist and version comparison — are pure functions, so they are tested on the JVM.
 */
class OtaUpdaterTest {

    @Test
    fun `https on an allowed host is accepted`() {
        assertTrue(OtaUpdater.isAllowedUrl("https://github.com/malys/MG4_Simple_Launcher/releases/download/unstable/a.apk"))
        assertTrue(OtaUpdater.isAllowedUrl("https://objects.githubusercontent.com/x"))
        assertTrue(OtaUpdater.isAllowedUrl("https://release-assets.githubusercontent.com/x"))
    }

    @Test
    fun `http is rejected even on an allowed host`() {
        assertFalse(OtaUpdater.isAllowedUrl("http://github.com/x.apk"))
    }

    @Test
    fun `lookalike hosts are rejected - the match is exact, not a suffix`() {
        assertFalse(OtaUpdater.isAllowedUrl("https://github.com.attacker.net/x.apk"))
        assertFalse(OtaUpdater.isAllowedUrl("https://evil-github.com/x.apk"))
        assertFalse(OtaUpdater.isAllowedUrl("https://raw.githubusercontent.com/x.apk"))
    }

    @Test
    fun `garbage urls are rejected rather than throwing`() {
        assertFalse(OtaUpdater.isAllowedUrl(""))
        assertFalse(OtaUpdater.isAllowedUrl("not a url"))
        assertFalse(OtaUpdater.isAllowedUrl("file:///data/local/tmp/x.apk"))
    }

    @Test
    fun `version comparison is numeric, not lexicographic`() {
        assertTrue(OtaUpdater.isNewer("1.4.10", "1.4.9"))
        assertFalse(OtaUpdater.isNewer("1.4.9", "1.4.10"))
        assertFalse(OtaUpdater.isNewer("1.4.1", "1.4.1"))
        assertTrue(OtaUpdater.isNewer("1.4.1.2-unstable", "1.4.1.1-unstable"))
    }

    @Test
    fun `non-numeric segments become zero instead of shifting later ones`() {
        assertArrayEquals(intArrayOf(1, 4, 0, 7), OtaUpdater.segments("1.4.x.7"))
        assertArrayEquals(intArrayOf(1, 4, 1), OtaUpdater.segments("v1.4.1-unstable"))
        assertArrayEquals(intArrayOf(1, 4, 1), OtaUpdater.segments("1.4.1+build9"))
    }

    @Test
    fun `version is read from the asset name, since the tag is always unstable`() {
        assertEquals("1.4.42", OtaUpdater.versionFromAssetName("MG4SimpleLauncher-unstable-1.4.42.apk"))
        assertEquals("1.4", OtaUpdater.versionFromAssetName("MG4SimpleLauncher-unstable-1.4.apk"))
        assertNull(OtaUpdater.versionFromAssetName("MG4SimpleLauncher-unstable.apk"))
        assertNull(OtaUpdater.versionFromAssetName("not-an-apk-1.2.3.zip"))
    }

    @Test
    fun `download file name is sanitised - a remote version never reaches a path raw`() {
        assertEquals(
            "MG4SimpleLauncher-unstable-1.4.42.apk",
            OtaUpdater.downloadFileName("1.4.42")
        )
        assertEquals(
            "MG4SimpleLauncher-unstable-.._.._etc_passwd.apk",
            OtaUpdater.downloadFileName("../../etc/passwd")
        )
        assertEquals("MG4SimpleLauncher-unstable-unknown.apk", OtaUpdater.downloadFileName(null))
    }

    @Test
    fun `pm install requires both success text and zero exit`() {
        assertTrue(OtaUpdater.installSucceeded(0, "Success\n"))
        assertFalse(OtaUpdater.installSucceeded(1, "Success\n"))
        assertFalse(OtaUpdater.installSucceeded(0, "Failure [INSTALL_FAILED]"))
    }

    private fun assertArrayEquals(expected: IntArray, actual: IntArray) =
        org.junit.Assert.assertArrayEquals(expected, actual)
}
