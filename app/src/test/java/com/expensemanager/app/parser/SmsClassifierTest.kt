package com.expensemanager.app.parser

import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsClassifierTest {

    @Test
    fun `test pure OTP messages are rejected`() {
        val otp1 = "Your OTP for login to NetBanking is 592819. Valid for 10 minutes. Do not share with anyone."
        val otp2 = "481920 is your verification code for Swiggy. Never share your OTP."
        val otp3 = "Use code 992101 to verify your mobile number. Valid for 5 mins."

        assertFalse(SmsClassifier.isFinancialSms(otp1))
        assertFalse(SmsClassifier.isFinancialSms(otp2))
        assertFalse(SmsClassifier.isFinancialSms(otp3))
    }

    @Test
    fun `test promotional and loan offers are rejected`() {
        val promo1 = "Congratulations! You have a Pre-approved Personal Loan of Rs. 5,00,000. Apply now: http://bit.ly/loan"
        val promo2 = "Get flat 50% off on your next order at Dominos. Use coupon FEAST50. Hurry!"
        val promo3 = "Exclusive credit limit increase offer! Upgrade your card today with 0 processing fee."

        assertFalse(SmsClassifier.isFinancialSms(promo1))
        assertFalse(SmsClassifier.isFinancialSms(promo2))
        assertFalse(SmsClassifier.isFinancialSms(promo3))
    }

    @Test
    fun `test valid financial debits are accepted`() {
        val debit1 = "Rs. 450.00 debited from A/c XX1234 on 24-AUG-26 by UPI to Swiggy. Avail Bal: Rs 14,200.00"
        val debit2 = "Spent Rs.2,499.00 on your ICICI Bank Credit Card ending 8091 at Amazon India on 24-Aug-26."
        val debit3 = "Sent Rs. 500 to rahul@okhdfcbank via UPI. Ref No: 629104819201. Bal Rs. 8,120.50"

        assertTrue(SmsClassifier.isFinancialSms(debit1))
        assertTrue(SmsClassifier.isFinancialSms(debit2))
        assertTrue(SmsClassifier.isFinancialSms(debit3))
    }

    @Test
    fun `test classification of debit vs credit vs refund`() {
        val debitSms = "Rs 1,200.00 debited from your A/c XX4412 on 24-Aug-26 at Zomato."
        val creditSms = "Rs 45,000.00 credited to your A/c XX4412 on 01-Aug-26 towards Salary. Avail Bal Rs 52,100.00"
        val refundSms = "Refund of Rs 350.00 credited to your A/c XX4412 for cancelled Uber ride. Txn ID: REF991201"

        assertEquals(TransactionType.DEBIT, SmsClassifier.classifyTransactionType(debitSms))
        assertEquals(TransactionType.CREDIT, SmsClassifier.classifyTransactionType(creditSms))
        assertEquals(TransactionType.REFUND, SmsClassifier.classifyTransactionType(refundSms))
    }

    @Test
    fun `test payment requests and pending mandates are rejected`() {
        val req1 = "Simpl has requested money from you on CRED. On approving the request, Rs.4,078.35 will be debited from your account."
        val req2 = "Jio Prepaid Rec... has requested money from you on your Kiwi app. On approving the request, Rs349.0 will be debited from your account."
        val req3 = "E-Mandate! Rs.250.00 will be deducted on 25/08/26 from HDFC Bank A/c 7011 for Groww mandate"

        assertFalse(SmsClassifier.isFinancialSms(req1))
        assertFalse(SmsClassifier.isFinancialSms(req2))
        assertFalse(SmsClassifier.isFinancialSms(req3))
    }

    @Test
    fun `test failed and declined transactions are rejected`() {
        val failed1 = "Your last payment attempt of Rs. 349.00 on Aug 12, 2025 10:02:24 AM for Jio number was unsuccessful. Please try again."
        val failed2 = "Transaction of Rs. 1,500.00 at Swiggy on your HDFC Bank Card XX7011 was declined due to insufficient funds."

        assertFalse(SmsClassifier.isFinancialSms(failed1))
        assertFalse(SmsClassifier.isFinancialSms(failed2))
    }

    @Test
    fun `test telecom data quota alerts are rejected`() {
        val data1 = "ATTENTION! 90% of available data used as on 11-Aug-25 17:34 ! Total Data quota as per plan : 1.50 GB Plan Name: Rs 198_14D"
        val data2 = "Recharge plan Expired on 09-Aug-25 10:04 Hrs ! Plan Name : Rs 198_14D_2GB/D Jio Number : 8778663767"

        assertFalse(SmsClassifier.isFinancialSms(data1))
        assertFalse(SmsClassifier.isFinancialSms(data2))
    }

    @Test
    fun `test bill due reminders and statement generated alerts are rejected`() {
        val billDue1 = "Payment of INR 17569.29 for Axis Bank Credit Card no. XX9117 is due on 30-08-26 with minimum amount due of INR 466. Ignore if paid."
        val billDue2 = "Statement Generated: For HDFC Bank Credit Card XX2942. View: https://1.hdfc.bank.in/HDFCBK/v/GkaXRF Pay: https://1.hdfc.bank.in"
        val billDue3 = "Alert: EMI of Rs. 36881 is due on 07-Aug-2026, for HDFC Bank Loan A/c 170660851. Please ensure sufficient balance."

        assertFalse(SmsClassifier.isFinancialSms(billDue1))
        assertFalse(SmsClassifier.isFinancialSms(billDue2))
        assertFalse(SmsClassifier.isFinancialSms(billDue3))
    }

    @Test
    fun `test loan marketing offers and disbursement consents are rejected`() {
        val loan1 = "Service Alert: Funds of INR 2,17,000.00 on YES BANK Credit Card ending 1006 are available and require consent to continue disbursement. ccybl.in/YESBNK -YES BANK LTD"
        val loan2 = "Dear Customer, please complete the pending steps in your Loan Against Mutual Fund journey. Continue now. https://uatlamf.shriramcredit.in/user/loans/?"
        val loan3 = "Your Personal Loan of Rs. 16,10,000/- is disbursed. After deduction of applicable charges, net amount of Rs. 12,38,531/- has been credited to your Bank account no. XXXXXXXXXX7011."

        assertFalse(SmsClassifier.isFinancialSms(loan1))
        assertFalse(SmsClassifier.isFinancialSms(loan2))
        assertFalse(SmsClassifier.isFinancialSms(loan3))
    }

    @Test
    fun `test retail promotional marketing ads and token discounts are rejected`() {
        val ad1 = "Last reminder Chennai, Itsy Bitsy VIVIRA Mall Grand Opening | Up to 80% OFF, biggest deals, FREE Ocean Resin Workshop on Rs.1000+ shopping. Call: 9845938061 Hurry!"
        val ad2 = "Hi Karthi, Get your preferred time slot for your house relocation by paying a token amount of Rs. 499.00 only. Click here to pay and reserve"
        val ad3 = "Your PhonePe Gift Card with number 8012510032735902498417010 with Rs. 18 expires in 45 days. Tap for details: https://phone.pe/PHONPE/wcti"

        assertFalse(SmsClassifier.isFinancialSms(ad1))
        assertFalse(SmsClassifier.isFinancialSms(ad2))
        assertFalse(SmsClassifier.isFinancialSms(ad3))
    }

    @Test
    fun `test stock exchange trade confirmations and demat reports are rejected`() {
        val trade1 = "BSE Trade Confirmation Client Code FB3008 - Broker 6498 - EQ Value Rs 0.00 - FNO Value Rs 216038.00 - Dated 19-08-2026"
        val trade2 = "Your trades executed on 21/08/2026 14 buy Rs 21526 14 sell Rs 21493 CLCode FB3008 Mem.code 56550"
        val trade3 = "ZERODHABROKINGLIMITED on 14-08-2026 reported your Fund bal Rs.16362.630 & Securities bal 0.000."
        val cdsl1 = "CDSL: Debit in a/c *01573896 for 54-GUJARAT THEMIS-EQ1/- on 08DEC"

        assertFalse(SmsClassifier.isFinancialSms(trade1))
        assertFalse(SmsClassifier.isFinancialSms(trade2))
        assertFalse(SmsClassifier.isFinancialSms(trade3))
        assertFalse(SmsClassifier.isFinancialSms(cdsl1))
    }

    @Test
    fun `test credit card statement alerts and due reminders are rejected`() {
        val stmt1 = "YES BANK Credit Card XX1006 AUG-26 statement: Total due INR 41192.45  Min due INR 11084.14 Due by 03-SEP-2026. Pay full outstanding to avoid charges."
        val stmt2 = "Your credit card bill for HDFC Bank XXXX-2942 has been generated.\n\nTotal amount: INR 1,399.00\nDue date: September 05, 2026"
        val stmt3 = "Amount Due\nRs.2459 on HDFC Bank Credit Card 2942. Pay instantly by 08/MAR/2026 via PayZapp"
        val stmt4 = "Your Axis Bank Credit Card no. XX9117 has an overdue. Pay the min due of INR 808.50 at https://ccm.axis.bank.in"

        assertFalse(SmsClassifier.isFinancialSms(stmt1))
        assertFalse(SmsClassifier.isFinancialSms(stmt2))
        assertFalse(SmsClassifier.isFinancialSms(stmt3))
        assertFalse(SmsClassifier.isFinancialSms(stmt4))
    }

    @Test
    fun `test credit limit revision and autopay activation are rejected`() {
        val limit1 = "The revised Credit Limit of your YES BANK Credit Card x1006 has been increased to Rs. 1,74,000.00."
        val autopay1 = "AutoPay activation:\nSUCCESSFUL\nTxn amt: INR 2.00\nMerchant: Microsoft Businesses\nAxis Bank Credit Card: XX9117\nMax limit: INR 200000.00"
        val emi1 = "Your Axis Visa Privilege Credit Card transaction of INR 25366 has been successfully converted into EMI."

        assertFalse(SmsClassifier.isFinancialSms(limit1))
        assertFalse(SmsClassifier.isFinancialSms(autopay1))
        assertFalse(SmsClassifier.isFinancialSms(emi1))
    }

    @Test
    fun `test epfo passbook and future advisories are rejected`() {
        val epfo1 = "Dear XXXXXXXX9857, your passbook balance against THVSH**************0820 is Rs. 1,13,416/-. Contribution of Rs. 2,350/- for due month Dec-25 has been received."
        val adv1 = "YOUR REMUNERATION OF RS. 82559 FOR THE MONTH OF MARCH-2026 HAS BEEN PROCESSED AND WILL BE CREDITED SHORTLY INTO YOUR HDFC BANK"
        val adv2 = "We have initiated a refund of Rs.611.00 for ORD69674043700 into your UPI, which should reflect in 3-5 business days."

        assertFalse(SmsClassifier.isFinancialSms(epfo1))
        assertFalse(SmsClassifier.isFinancialSms(adv1))
        assertFalse(SmsClassifier.isFinancialSms(adv2))
    }

    @Test
    fun `test telecom mobile recharge confirmations and promotional cashback pitches are rejected`() {
        val promo1 = "Recharge & earn upto Rs. 200 cashback. Open Airtel app now https://i.airtel.in/upto200back_1"
        val promo2 = "Ready to recharge? Do it smarter. Get up to Rs. 200 cashback on Airtel app via MobiKwik UPI on a minimum payment of Rs. 150. Recharge now: https://i.airtel.in/Cashback_Offers_1"
        val promo3 = "Important Update for 8056156721: Your Prepaid bill payment of Rs.349 was successful. You could've paid up to Rs.87 less with Airtel Axis Bank Credit Card. View Details https://i.airtel.in/AxisCard16"
        val recharge1 = "Hi, recharge of Rs. 399 successfully credited to your Airtel number 8056156721, also the validity has been extended till 26-07-2026."

        assertFalse(SmsClassifier.isFinancialSms(promo1))
        assertFalse(SmsClassifier.isFinancialSms(promo2))
        assertFalse(SmsClassifier.isFinancialSms(promo3))
        assertFalse(SmsClassifier.isFinancialSms(recharge1))
    }
}

