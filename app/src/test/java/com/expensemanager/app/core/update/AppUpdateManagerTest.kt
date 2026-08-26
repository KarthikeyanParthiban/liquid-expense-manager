package com.expensemanager.app.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `test semantic version comparison`() {
        assertTrue(AppUpdateManager.isVersionNewer("1.1.2", "1.1.1"))
        assertTrue(AppUpdateManager.isVersionNewer("1.2.0", "1.1.9"))
        assertTrue(AppUpdateManager.isVersionNewer("2.0.0", "1.9.9"))
        assertTrue(AppUpdateManager.isVersionNewer("1.1.1.1", "1.1.1"))

        assertFalse(AppUpdateManager.isVersionNewer("1.1.1", "1.1.1"))
        assertFalse(AppUpdateManager.isVersionNewer("1.1.0", "1.1.1"))
        assertFalse(AppUpdateManager.isVersionNewer("1.0.9", "1.1.0"))
    }
}
