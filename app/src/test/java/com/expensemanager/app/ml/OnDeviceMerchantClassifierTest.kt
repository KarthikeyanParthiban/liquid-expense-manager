package com.expensemanager.app.ml

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.parser.CategoryClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class OnDeviceMerchantClassifierTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setup() {
            val assetFile = File("src/main/assets/models/merchant_classifier_weights.json").takeIf { it.exists() }
                ?: File("app/src/main/assets/models/merchant_classifier_weights.json")

            assertTrue("Weights file must exist at ${assetFile.absolutePath}", assetFile.exists())
            OnDeviceMerchantClassifier.loadFromStream(FileInputStream(assetFile))
        }
    }

    @Test
    fun `test ML classification for unseen long-tail Food merchants`() {
        val p1 = OnDeviceMerchantClassifier.predict("Sharma Sweet House")
        assertNotNull(p1)
        assertEquals(Category.FOOD, p1!!.category)
        assertTrue("Confidence should be above 40%", p1.confidence >= 0.40f)

        val p2 = OnDeviceMerchantClassifier.predict("Amma Canteen Meals")
        assertNotNull(p2)
        assertEquals(Category.FOOD, p2!!.category)

        val p3 = OnDeviceMerchantClassifier.predict("Theobroma Patisserie & Bakery")
        assertNotNull(p3)
        assertEquals(Category.FOOD, p3!!.category)
    }

    @Test
    fun `test ML classification for unseen Groceries and Kirana stores`() {
        val p1 = OnDeviceMerchantClassifier.predict("Gupta Kirana Store")
        assertNotNull(p1)
        assertEquals(Category.GROCERIES, p1!!.category)

        val p2 = OnDeviceMerchantClassifier.predict("Daily Needs Supermarket")
        assertNotNull(p2)
        assertEquals(Category.GROCERIES, p2!!.category)

        val p3 = OnDeviceMerchantClassifier.predict("Fresh Vegetable Mart")
        assertNotNull(p3)
        assertEquals(Category.GROCERIES, p3!!.category)
    }

    @Test
    fun `test ML classification for unseen Transport and Fuel outlets`() {
        val p1 = OnDeviceMerchantClassifier.predict("Ramesh Cabs Travels")
        assertNotNull(p1)
        assertEquals(Category.TRANSPORT, p1!!.category)

        val p2 = OnDeviceMerchantClassifier.predict("Indian Oil Petrol Bunk")
        assertNotNull(p2)
        assertEquals(Category.TRANSPORT, p2!!.category)

        val p3 = OnDeviceMerchantClassifier.predict("Ather EV Charging Station")
        assertNotNull(p3)
        assertEquals(Category.TRANSPORT, p3!!.category)
    }

    @Test
    fun `test ML classification for unseen Health and Diagnostics clinics`() {
        val p1 = OnDeviceMerchantClassifier.predict("Apollo Medico Pharmacy")
        assertNotNull(p1)
        assertEquals(Category.HEALTHCARE, p1!!.category)

        val p2 = OnDeviceMerchantClassifier.predict("Dr Lal Pathlabs Blood Test")
        assertNotNull(p2)
        assertEquals(Category.HEALTHCARE, p2!!.category)

        val p3 = OnDeviceMerchantClassifier.predict("Dental Care Root Canal Clinic")
        assertNotNull(p3)
        assertEquals(Category.HEALTHCARE, p3!!.category)
    }

    @Test
    fun `test ML classification for unseen Education and Coaching centers`() {
        val p1 = OnDeviceMerchantClassifier.predict("Kendriya Vidyalaya School Fees")
        assertNotNull(p1)
        assertEquals(Category.EDUCATION, p1!!.category)

        val p2 = OnDeviceMerchantClassifier.predict("Brilliant Coaching Center NEET")
        assertNotNull(p2)
        assertEquals(Category.EDUCATION, p2!!.category)

        val p3 = OnDeviceMerchantClassifier.predict("College Semester Tuition Fees")
        assertNotNull(p3)
        assertEquals(Category.EDUCATION, p3!!.category)
    }

    @Test
    fun `test ML classification for unseen Personal Care and Grooming salons`() {
        val p1 = OnDeviceMerchantClassifier.predict("Looks Unisex Hair Parlour")
        assertNotNull(p1)
        assertEquals(Category.PERSONAL, p1!!.category)

        val p2 = OnDeviceMerchantClassifier.predict("Ayur Care Spa & Massage")
        assertNotNull(p2)
        assertEquals(Category.PERSONAL, p2!!.category)

        val p3 = OnDeviceMerchantClassifier.predict("Grooming Lounge Mens Barber")
        assertNotNull(p3)
        assertEquals(Category.PERSONAL, p3!!.category)
    }

    @Test
    fun `test ML classification for unseen Entertainment and Gaming`() {
        val p1 = OnDeviceMerchantClassifier.predict("PVR Cinemas Movie Ticket")
        assertNotNull(p1)
        assertEquals(Category.ENTERTAINMENT, p1!!.category)

        val p2 = OnDeviceMerchantClassifier.predict("Timezone Bowling Arcade")
        assertNotNull(p2)
        assertEquals(Category.ENTERTAINMENT, p2!!.category)
    }

    @Test
    fun `test ML integration into CategoryClassifier Tier-3 pipeline`() {
        val result = CategoryClassifier.classifyWithReason(
            merchantOrVpa = "Sharma Sweet House",
            messageBody = "Rs 250 paid to Sharma Sweet House on 26Aug26",
            transactionType = TransactionType.DEBIT
        )

        assertEquals(Category.FOOD, result.category)
        assertTrue(
            "Reason should indicate match: ${result.reason}",
            result.reason.contains("ML Model", ignoreCase = true) ||
                    result.reason.contains("Food", ignoreCase = true) ||
                    result.reason.contains("sweet", ignoreCase = true)
        )
    }
}
