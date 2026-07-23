package com.aaron.sidegesture.platform.userprofile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.ktx.launchAppInfo
import com.aaron.sidegesture.ktx.qualifiedName
import com.aaron.sidegesture.utils.AppInfoUtils
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileAppManagerInstrumentedTest {

    @Test
    fun associatedProfileAppAppearsInActionSelectSourceAndLaunchesNormally() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val associatedApps = ProfileAppManager.queryAssociatedProfileApps(context)
        val profileApp = associatedApps.firstOrNull { appInfo ->
            appInfo.packageName == context.packageName
        }
        assumeTrue(
            "Target package is not installed in an associated profile",
            profileApp != null
        )
        requireNotNull(profileApp)

        val allApps = AppInfoUtils.queryLauncherActivities(
            context = context,
            includeAssociatedProfiles = true
        )
        val currentApp = allApps.first { appInfo ->
            appInfo.packageName == context.packageName &&
                appInfo.profileSerialNumber == null
        }

        assertTrue(profileApp.profileSerialNumber != null)
        assertTrue(allApps.any { it.qualifiedName == profileApp.qualifiedName })
        assertNotEquals(currentApp.qualifiedName, profileApp.qualifiedName)
        assertNotNull(ProfileAppManager.loadBadgedIcon(context, profileApp))
        assertTrue(context.launchAppInfo(profileApp))
    }
}
