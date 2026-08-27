package com.expensemanager.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `test semantic version comparison`() {
        // Newer remote versions
        assertTrue(AppUpdateManager.isVersionNewer("1.1.2", "1.1.1"))
        assertTrue(AppUpdateManager.isVersionNewer("1.2.0", "1.1.9"))
        assertTrue(AppUpdateManager.isVersionNewer("2.0.0", "1.9.9"))
        assertTrue(AppUpdateManager.isVersionNewer("1.1.1.1", "1.1.1"))
        assertTrue(AppUpdateManager.isVersionNewer("v1.2.1", "1.2.0"))
        assertTrue(AppUpdateManager.isVersionNewer("v1.2.1-beta", "1.2.0"))

        // Equal versions (should NEVER trigger update)
        assertFalse(AppUpdateManager.isVersionNewer("1.1.1", "1.1.1"))
        assertFalse(AppUpdateManager.isVersionNewer("1.2.0", "1.2.0"))
        assertFalse(AppUpdateManager.isVersionNewer("v1.2.0", "1.2.0"))
        assertFalse(AppUpdateManager.isVersionNewer("v1.2.0", "v1.2.0"))
        assertFalse(AppUpdateManager.isVersionNewer("1.2.0-rc1", "1.2.0"))

        // Older remote versions
        assertFalse(AppUpdateManager.isVersionNewer("1.1.0", "1.1.1"))
        assertFalse(AppUpdateManager.isVersionNewer("1.0.9", "1.1.0"))
        assertFalse(AppUpdateManager.isVersionNewer("1.1.1", "1.2.0"))
    }

    @Test
    fun `test parseVersionNumbers extracts correct segments`() {
        assertEquals(listOf(1, 2, 0), AppUpdateManager.parseVersionNumbers("1.2.0"))
        assertEquals(listOf(1, 2, 0), AppUpdateManager.parseVersionNumbers("v1.2.0"))
        assertEquals(listOf(1, 2, 0), AppUpdateManager.parseVersionNumbers("V1.2.0-beta"))
        assertEquals(listOf(1, 2, 0, 1), AppUpdateManager.parseVersionNumbers("1.2.0.1"))
    }
}
