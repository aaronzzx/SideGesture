package com.aaron.sidegesture.feature.update

import com.aaron.sidegesture.entity.GithubRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun isRemoteNewerHandlesHigherEqualLowerAndMissingSegments() {
        assertTrue(UpdateChecker.isRemoteNewer("v1.5.5", "1.5.4"))
        assertFalse(UpdateChecker.isRemoteNewer("1.5.4", "V1.5.4"))
        assertFalse(UpdateChecker.isRemoteNewer("1.5.3", "1.5.4"))
        assertTrue(UpdateChecker.isRemoteNewer("1.5.1", "1.5"))
        assertFalse(UpdateChecker.isRemoteNewer("1.5", "1.5.1"))
        assertFalse(UpdateChecker.isRemoteNewer("1.5", "1.5.0"))
    }

    @Test
    fun displayVersionNormalizesVPrefix() {
        assertEquals("v1.5.4", UpdateChecker.displayVersion("1.5.4"))
        assertEquals("v1.5.4", UpdateChecker.displayVersion("v1.5.4"))
        assertEquals("v1.5.4", UpdateChecker.displayVersion("V1.5.4"))
        assertEquals("v1.5.4", UpdateChecker.displayVersion("  1.5.4  "))
    }

    @Test
    fun pickApkAssetMatchesExtensionIgnoringCase() {
        val apk = GithubRelease.Asset(name = "SideGesture.APK", browserDownloadUrl = "https://example.invalid/app")
        val release = GithubRelease(
            assets = listOf(
                GithubRelease.Asset(name = "checksums.txt"),
                apk
            )
        )

        assertNotNull(UpdateChecker.pickApkAsset(release))
        assertEquals(apk, UpdateChecker.pickApkAsset(release))
    }

    @Test
    fun pickApkAssetReturnsNullWhenReleaseHasNoApk() {
        val release = GithubRelease(
            assets = listOf(GithubRelease.Asset(name = "source.zip"))
        )

        assertNull(UpdateChecker.pickApkAsset(release))
    }
}
