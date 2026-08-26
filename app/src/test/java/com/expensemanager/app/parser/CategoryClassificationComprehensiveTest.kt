package com.expensemanager.app.parser

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryClassificationComprehensiveTest {

    @Test
    fun `test Food and Dining comprehensive brands`() {
        val testCases = listOf(
            Pair("ZOMATO", "Rs 350.00 debited from A/c XX1234 to ZOMATO UPI Ref 123456"),
            Pair("SWIGGY", "Rs 450.00 debited for Swiggy food order"),
            Pair("DOMINOS PIZZA", "Spent Rs 599 at Dominos Pizza on Card XX1006"),
            Pair("MCDONALDS", "INR 299 spent at McDonalds India"),
            Pair("KFC INDIA", "Paid Rs 450 to KFC Restaurant"),
            Pair("STARBUCKS COFFEE", "Spent INR 395.00 on Card XX9117 at STARBUCKS"),
            Pair("CHAAYOS", "Paid Rs 180 to Chaayos Cafe"),
            Pair("CHAI POINT", "Rs 90 debited for Chai Point order"),
            Pair("HALDIRAMS", "Spent Rs 650 at Haldirams Sweets and Restaurant"),
            Pair("BARBEQUE NATION", "Paid Rs 2,400 to Barbeque Nation Dining"),
            Pair("BEHROUZ BIRYANI", "Rs 520 debited to Behrouz Biryani"),
            Pair("BURGER KING", "Spent Rs 249 at Burger King"),
            Pair("THEOBROMA", "Paid Rs 420 to Theobroma Bakery & Pastries"),
            Pair("MEGHANA FOODS", "Rs 750 debited to Meghana Foods Biryani"),
            Pair("NATURALS ICE CREAM", "Spent Rs 160 at Naturals Ice Cream")
        )

        for ((merchant, sms) in testCases) {
            val result = CategoryClassifier.classify(merchant, sms, TransactionType.DEBIT)
            assertEquals("Expected FOOD for merchant: $merchant", Category.FOOD, result)
        }
    }

    @Test
    fun `test Groceries & Quick Commerce comprehensive brands`() {
        val testCases = listOf(
            Pair("ZEPTO", "INR 194.00 spent on Card XX9117 PHP*Zepto"),
            Pair("BLINKIT", "INR 896.00 spent on YES BANK Card X1006 @UPI_BLINKIT"),
            Pair("SWIGGY INSTAMART", "Rs 420.00 debited for Swiggy Instamart groceries"),
            Pair("BIGBASKET", "Paid Rs 1,450 to BigBasket Supermarket"),
            Pair("BB DAILY", "Rs 150 debited to BB Daily for morning milk"),
            Pair("DMART READY", "Spent Rs 3,200 at DMart Ready"),
            Pair("JIOMART", "Paid Rs 890 to JioMart Grocery"),
            Pair("NATURES BASKET", "Spent Rs 1,120 at Natures Basket"),
            Pair("LICIOUS", "Rs 490 debited to Licious Meat and Seafood"),
            Pair("FRESHTOHOME", "Spent Rs 560 at FreshToHome"),
            Pair("COUNTRY DELIGHT", "Paid Rs 340 to Country Delight Milk"),
            Pair("SPENCERS", "Spent Rs 1,800 at Spencers Supermarket")
        )

        for ((merchant, sms) in testCases) {
            val result = CategoryClassifier.classify(merchant, sms, TransactionType.DEBIT)
            assertEquals("Expected GROCERIES for merchant: $merchant", Category.GROCERIES, result)
        }
    }

    @Test
    fun `test Shopping comprehensive brands`() {
        val testCases = listOf(
            Pair("AMAZON RETAIL", "Spent INR 3,999.00 on ICICI Bank Card ending 1044 at AMAZON RETAIL"),
            Pair("FLIPKART", "Paid Rs 1,899 to Flipkart India for electronics"),
            Pair("MYNTRA", "Spent Rs 2,450 on Myntra Designs fashion"),
            Pair("AJIO", "Paid Rs 1,299 to Ajio Trends apparel"),
            Pair("NYKAA", "Spent Rs 850 at Nykaa Cosmetics"),
            Pair("TATA CLIQ", "Paid Rs 3,400 to Tata CliQ luxury"),
            Pair("DECATHLON", "Spent Rs 1,499 at Decathlon Sports"),
            Pair("CROMA", "Spent Rs 12,990 at Croma Electronics"),
            Pair("RELIANCE DIGITAL", "Paid Rs 4,500 to Reliance Digital"),
            Pair("LENSKART", "Spent Rs 2,000 at Lenskart Eyewear"),
            Pair("ZARA", "Spent Rs 4,990 at Zara Retail"),
            Pair("H&M", "Paid Rs 2,299 to H&M Clothing"),
            Pair("TANISHQ", "Spent Rs 45,000 at Tanishq Jewellers"),
            Pair("CARATLANE", "Paid Rs 15,000 to CaratLane Jewellery"),
            Pair("MALABAR GOLD", "Spent Rs 52,000 at Malabar Gold and Diamonds"),
            Pair("IKEA", "Spent Rs 6,800 at IKEA Furniture")
        )

        for ((merchant, sms) in testCases) {
            val result = CategoryClassifier.classify(merchant, sms, TransactionType.DEBIT)
            assertEquals("Expected SHOPPING for merchant: $merchant", Category.SHOPPING, result)
        }
    }

    @Test
    fun `test Transport, Travel and Fuel brands`() {
        val testCases = listOf(
            Pair("UBER", "Rs 340.00 debited for Uber trip"),
            Pair("OLA CABS", "Paid Rs 280 to Ola Cabs"),
            Pair("RAPIDO", "Spent Rs 65 with Rapido Bike Taxi"),
            Pair("BLUSMART", "Paid Rs 410 to BluSmart Mobility"),
            Pair("NAMMA YATRI", "Rs 120 paid to Namma Yatri Auto"),
            Pair("IRCTC", "Spent Rs 1,250 on IRCTC Train Ticket"),
            Pair("DMRC METRO", "Paid Rs 40 for Delhi Metro Smart Card recharge"),
            Pair("BMRC METRO", "Spent Rs 50 at Namma Metro Bangalore"),
            Pair("MAKEMYTRIP", "Paid Rs 6,500 to MakeMyTrip for flight booking"),
            Pair("INDIGO AIRLINES", "Spent Rs 4,800 at IndiGo Airlines"),
            Pair("FASTAG", "Rs 500 debited for NHAI NETC FASTag toll plaza"),
            Pair("IOCL PETROL", "Spent Rs 2,000 at Indian Oil Petrol Pump"),
            Pair("BPCL FUEL", "Paid Rs 1,500 to Bharat Petroleum"),
            Pair("HPCL PUMP", "Spent Rs 1,800 at HPCL Fuel Station"),
            Pair("SHELL FUEL", "Paid Rs 2,200 at Shell Petrol Pump")
        )

        for ((merchant, sms) in testCases) {
            val result = CategoryClassifier.classify(merchant, sms, TransactionType.DEBIT)
            assertEquals("Expected TRANSPORT for merchant: $merchant", Category.TRANSPORT, result)
        }
    }

    @Test
    fun `test Bills and Utilities providers`() {
        val testCases = listOf(
            Pair("AIRTEL POSTPAID", "Paid Rs 589 to Airtel Postpaid mobile bill"),
            Pair("JIO FIBER", "Rs 1,179 debited for JioFiber broadband recharge"),
            Pair("VI PREPAID", "Spent Rs 299 for Vi mobile recharge"),
            Pair("ACT FIBERNET", "Paid Rs 1,050 to ACT Fibernet internet"),
            Pair("TATA PLAY", "Spent Rs 450 on Tata Play DTH subscription"),
            Pair("BESCOM", "Paid Rs 2,340 for BESCOM electricity bill"),
            Pair("TATA POWER", "Spent Rs 1,890 on Tata Power electricity bill"),
            Pair("ADANI ELECTRICITY", "Paid Rs 3,100 to Adani Electricity Mumbai"),
            Pair("IGL GAS", "Paid Rs 850 for Indraprastha Gas piped gas bill"),
            Pair("NOBROKER RENT", "Rs 25,000 debited for NoBroker house rent payment"),
            Pair("MYGATE MAINTENANCE", "Paid Rs 4,500 for MyGate apartment society maintenance"),
            Pair("GOOGLE INDIA DIGITAL", "Sent Rs.350.90 to Google India Digital Serv Ref 623639338712")
        )

        for ((merchant, sms) in testCases) {
            val result = CategoryClassifier.classify(merchant, sms, TransactionType.DEBIT)
            assertEquals("Expected BILLS_UTILITIES for merchant: $merchant", Category.BILLS_UTILITIES, result)
        }
    }

    @Test
    fun `test Entertainment & OTT subscriptions`() {
        val testCases = listOf(
            Pair("NETFLIX", "Rs 649 debited for Netflix monthly subscription"),
            Pair("SPOTIFY", "Paid Rs 119 to Spotify Premium"),
            Pair("DISNEY HOTSTAR", "Spent Rs 899 at Disney+ Hotstar"),
            Pair("BOOKMYSHOW", "Spent Rs 640 at BookMyShow for movie tickets"),
            Pair("PVR CINEMAS", "Paid Rs 850 at PVR INOX Cinemas"),
            Pair("YOUTUBE PREMIUM", "Rs 149 debited for YouTube Premium"),
            Pair("SONYLIV", "Spent Rs 299 at SonyLIV"),
            Pair("STEAM GAMES", "Paid Rs 1,499 to Steam Games Store"),
            Pair("PLAYSTATION NETWORK", "Spent Rs 2,999 at PlayStation Store")
        )

        for ((merchant, sms) in testCases) {
            val result = CategoryClassifier.classify(merchant, sms, TransactionType.DEBIT)
            assertEquals("Expected ENTERTAINMENT for merchant: $merchant", Category.ENTERTAINMENT, result)
        }
    }

    @Test
    fun `test Healthcare & Pharmacy`() {
        val testCases = listOf(
            Pair("APOLLO PHARMACY", "Spent Rs 450 at Apollo Pharmacy for medicines"),
            Pair("TATA 1MG", "Paid Rs 780 to Tata 1mg Pharmacy"),
            Pair("PHARMEASY", "Spent Rs 1,200 at Pharmeasy medicine order"),
            Pair("MEDPLUS", "Paid Rs 350 to Medplus Chemist"),
            Pair("PRACTO", "Paid Rs 600 for Practo doctor consultation"),
            Pair("DR LAL PATHLABS", "Spent Rs 1,500 at Dr Lal PathLabs for blood test"),
            Pair("MAX HEALTHCARE", "Paid Rs 4,500 to Max Healthcare Hospital"),
            Pair("FORTIS HOSPITAL", "Paid Rs 6,200 at Fortis Hospital")
        )

        for ((merchant, sms) in testCases) {
            val result = CategoryClassifier.classify(merchant, sms, TransactionType.DEBIT)
            assertEquals("Expected HEALTHCARE for merchant: $merchant", Category.HEALTHCARE, result)
        }
    }

    @Test
    fun `test Investments & Wealth`() {
        val testCases = listOf(
            Pair("ZERODHA BROKING", "Rs 10,000 debited towards Zerodha Broking"),
            Pair("GROWW INVEST", "Rs 5,000 debited for Groww mutual fund SIP"),
            Pair("UPSTOX", "Paid Rs 2,500 to Upstox Securities"),
            Pair("INDMONEY", "Spent Rs 5,000 with INDmoney US Stocks"),
            Pair("CAMSONLINE", "Rs 2,000 debited for CAMS Mutual Fund SIP"),
            Pair("SBI MUTUAL FUND", "Rs 3,000 debited for SBI Mutual Fund investment"),
            Pair("HDFC LIFE", "Paid Rs 12,000 for HDFC Life Insurance premium"),
            Pair("STAR HEALTH", "Paid Rs 18,000 for Star Health Insurance policy")
        )

        for ((merchant, sms) in testCases) {
            val result = CategoryClassifier.classify(merchant, sms, TransactionType.DEBIT)
            assertEquals("Expected INVESTMENT for merchant: $merchant", Category.INVESTMENT, result)
        }
    }

    @Test
    fun `test UPI VPA Tokenization and Subdomain decomposition`() {
        val vpa1 = "swiggy.rzp@icici"
        val vpa2 = "blinkit.store@okaxis"
        val vpa3 = "hpcl.retail@icici"
        val vpa4 = "tatapower@billdesk"
        val vpa5 = "cultfit@axisbank"
        val vpa6 = "zomato.online@hdfcbank"
        val vpa7 = "apollo.pharmacy@ybl"

        assertEquals(Category.FOOD, CategoryClassifier.classify(vpa1, "Sent Rs 250 to VPA $vpa1", TransactionType.DEBIT))
        assertEquals(Category.GROCERIES, CategoryClassifier.classify(vpa2, "Sent Rs 450 to VPA $vpa2", TransactionType.DEBIT))
        assertEquals(Category.TRANSPORT, CategoryClassifier.classify(vpa3, "Paid Rs 2000 to VPA $vpa3", TransactionType.DEBIT))
        assertEquals(Category.BILLS_UTILITIES, CategoryClassifier.classify(vpa4, "Paid Rs 1500 to VPA $vpa4", TransactionType.DEBIT))
        assertEquals(Category.PERSONAL, CategoryClassifier.classify(vpa5, "Paid Rs 3000 to VPA $vpa5", TransactionType.DEBIT))
        assertEquals(Category.FOOD, CategoryClassifier.classify(vpa6, "Paid Rs 600 to VPA $vpa6", TransactionType.DEBIT))
        assertEquals(Category.HEALTHCARE, CategoryClassifier.classify(vpa7, "Paid Rs 350 to VPA $vpa7", TransactionType.DEBIT))
    }

    @Test
    fun `test Peer-to-Peer UPI transfers are categorized as TRANSFERS instead of OTHERS`() {
        val p2pSms1 = "Rs 3000.00 sent via UPI on 17-06-2026 at 08:59:07 to KARISHMA P.Ref:616868340519"
        val p2pSms2 = "Rs 500.00 transferred to Ramesh Kumar via UPI Ref: 123456"
        val p2pSms3 = "Rs 1500.00 debited by transfer of Rs 1500 to Priya S on 24Aug26"

        val result1 = CategoryClassifier.classify("KARISHMA P", p2pSms1, TransactionType.DEBIT)
        val result2 = CategoryClassifier.classify("Ramesh Kumar", p2pSms2, TransactionType.DEBIT)
        val result3 = CategoryClassifier.classify("Priya S", p2pSms3, TransactionType.DEBIT)

        assertEquals("P2P transfer should map to TRANSFERS", Category.TRANSFERS, result1)
        assertEquals("P2P transfer should map to TRANSFERS", Category.TRANSFERS, result2)
        assertEquals("P2P transfer should map to TRANSFERS", Category.TRANSFERS, result3)
    }

    @Test
    fun `test contextual multi-vertical disambiguation accuracy`() {
        // Amazon
        assertEquals(Category.GROCERIES, CategoryClassifier.classify("Amazon Fresh", "Spent Rs 850 at Amazon Fresh", TransactionType.DEBIT))
        assertEquals(Category.ENTERTAINMENT, CategoryClassifier.classify("Amazon Prime", "Spent Rs 1499 for Amazon Prime subscription", TransactionType.DEBIT))
        assertEquals(Category.BILLS_UTILITIES, CategoryClassifier.classify("Amazon Pay", "Paid electricity bill on Amazon Pay", TransactionType.DEBIT))
        assertEquals(Category.SHOPPING, CategoryClassifier.classify("Amazon Retail", "Spent Rs 2500 on Amazon Shopping", TransactionType.DEBIT))

        // Jio
        assertEquals(Category.GROCERIES, CategoryClassifier.classify("JioMart", "Spent Rs 600 at JioMart groceries", TransactionType.DEBIT))
        assertEquals(Category.ENTERTAINMENT, CategoryClassifier.classify("JioCinema", "Paid Rs 89 for JioCinema premium", TransactionType.DEBIT))
        assertEquals(Category.TRANSPORT, CategoryClassifier.classify("Jio-bp", "Paid Rs 1500 at Jio-bp petrol pump", TransactionType.DEBIT))
        assertEquals(Category.BILLS_UTILITIES, CategoryClassifier.classify("Jio Recharge", "Paid Rs 349 for Jio prepaid recharge", TransactionType.DEBIT))
    }
}
