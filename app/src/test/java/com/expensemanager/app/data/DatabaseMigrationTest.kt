package com.expensemanager.app.data

import com.expensemanager.app.data.local.ExpenseDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun `test MIGRATION_1_2 configuration and version targets`() {
        val migration = ExpenseDatabase.MIGRATION_1_2
        assertNotNull(migration)
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }
}
