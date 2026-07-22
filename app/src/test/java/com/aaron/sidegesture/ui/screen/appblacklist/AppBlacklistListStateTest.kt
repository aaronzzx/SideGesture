package com.aaron.sidegesture.ui.screen.appblacklist

import com.aaron.sidegesture.entity.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBlacklistListStateTest {

    @Test
    fun appsStayHiddenUntilSavedSelectionLoads() {
        val appsLoadedState = AppBlacklistListState().loadAppInfos(
            listOf(
                appInfo("app.alpha", "Alpha"),
                appInfo("app.beta", "Beta")
            )
        )

        assertTrue(appsLoadedState.visibleAppInfos.isEmpty())

        val readyState = appsLoadedState.loadSavedSelection(listOf("app.beta"))

        assertEquals(listOf("app.beta", "app.alpha"), readyState.visibleAppInfos.packageNames())
    }

    @Test
    fun savedSelectionSurvivesWhenLoadedBeforeApps() {
        val state = AppBlacklistListState()
            .loadSavedSelection(listOf("app.beta", "app.missing"))
            .loadAppInfos(
                listOf(
                    appInfo("app.alpha", "Alpha"),
                    appInfo("app.beta", "Beta")
                )
            )

        assertEquals(listOf("app.beta", "app.missing"), state.excludeApps)
        assertEquals(listOf("app.beta", "app.alpha"), state.visibleAppInfos.packageNames())
        assertTrue("app.beta" in state.excludeApps)
    }

    @Test
    fun savedSelectionSurvivesWhenAppsLoadBeforeSelection() {
        val state = AppBlacklistListState()
            .loadAppInfos(
                listOf(
                    appInfo("app.alpha", "Alpha"),
                    appInfo("app.beta", "Beta")
                )
            )
            .loadSavedSelection(listOf("app.beta"))

        assertEquals(listOf("app.beta"), state.excludeApps)
        assertEquals(listOf("app.beta", "app.alpha"), state.visibleAppInfos.packageNames())
    }

    @Test
    fun selectionChangesDoNotReorderAppsDuringCurrentSession() {
        val initialState = AppBlacklistListState()
            .loadSavedSelection(listOf("app.beta"))
            .loadAppInfos(
                listOf(
                    appInfo("app.alpha", "Alpha"),
                    appInfo("app.beta", "Beta"),
                    appInfo("app.charlie", "Charlie")
                )
            )
        val originalOrder = initialState.visibleAppInfos.packageNames()

        val changedState = initialState
            .selectApp("app.charlie", selected = true)
            .selectApp("app.beta", selected = false)

        assertEquals(listOf("app.beta", "app.alpha", "app.charlie"), originalOrder)
        assertEquals(originalOrder, changedState.visibleAppInfos.packageNames())
        assertEquals(listOf("app.charlie"), changedState.excludeApps)
    }

    @Test
    fun nextEntryReordersUsingLatestSavedSelection() {
        val state = AppBlacklistListState()
            .loadSavedSelection(listOf("app.charlie"))
            .loadAppInfos(
                listOf(
                    appInfo("app.alpha", "Alpha"),
                    appInfo("app.beta", "Beta"),
                    appInfo("app.charlie", "Charlie")
                )
            )

        assertEquals(
            listOf("app.charlie", "app.alpha", "app.beta"),
            state.visibleAppInfos.packageNames()
        )
    }

    @Test
    fun searchKeepsSessionOrderAndCanonicalSelection() {
        val state = AppBlacklistListState()
            .loadSavedSelection(listOf("app.beta", "app.missing"))
            .loadAppInfos(
                listOf(
                    appInfo("app.alpha", "Alpha"),
                    appInfo("app.beta", "Beta"),
                    appInfo("app.charlie", "Charlie")
                )
            )
            .updateSearchQuery("a")

        assertEquals(
            listOf("app.beta", "app.alpha", "app.charlie"),
            state.visibleAppInfos.packageNames()
        )
        assertEquals(listOf("app.beta", "app.missing"), state.excludeApps)
        assertTrue(state.isSelected("app.beta"))
        assertFalse(state.isSelected("app.alpha"))
    }

    private fun appInfo(packageName: String, label: String): AppInfo {
        return AppInfo(
            packageName = packageName,
            className = "$packageName.MainActivity",
            label = label
        )
    }

    private fun List<AppInfo>.packageNames(): List<String> = map(AppInfo::packageName)
}
