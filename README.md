# 🍏 Liquid Expense Manager

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=flat&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Repository%20%2B%20Coroutines%20Flow-blue)](https://developer.android.com)
[![Database](https://img.shields.io/badge/Storage-Room%20Database%20(Local%20Only)-00599C?style=flat&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

An intelligent, privacy-first, on-device expense manager for Android that transforms transactional SMS messages from Indian banks, credit cards, and UPI apps into actionable financial analytics with an **Apple-inspired Light Mode Liquid UI**.

---

## ✨ Key Features

### 1. 🧠 Intelligent Multi-Channel SMS Parser & Deduplication
- **Deduplication Engine**: Reconciles dual-channel alerts (e.g. Bank SMS + UPI app notification, or UPI debit + Credit Card payment settlement) into a single consolidated transaction using:
  - Exact Reference / UTR matching
  - SHA-256 body checksums
  - Multi-channel fuzzy time-window clustering ($\Delta t \le 20\text{ mins}$)
  - Credit card bill settlement reconciliation ($\Delta t \le 60\text{ mins}$)
- **Bill Due vs Payment Reconciliation**: Distinguishes between statement dues (`due on 30-08-26`) and executed debits (`paid Rs. 1,003 to CRED Club`), ensuring bills are never double-counted.
- **Real-Time Point-in-Time Balance Architecture**: Decouples standalone daily balance broadcasts from transaction records with timestamp precedence to ensure primary hero balance matches actual bank reality.

### 2. 🚫 Zero-Tolerance Spam, Loan & Ad Exclusion
- Automatically rejects over 3,500+ non-transactional messages:
  - **Loan Marketing & Jumbo Consents**: Rejects credit card loan offers (`Funds of INR 2,17,000 available... require consent to continue disbursement`).
  - **Promotional Ads & Clickbaits**: Rejects store grand openings, voucher discounts (`Up to 80% OFF`, `Token amount of Rs. 499`).
  - **Stock Market & Demat Summaries**: Rejects BSE/NSE/MCX daily traded value summaries and broker margin broadcasts.
  - **Telecom & Travel Updates**: Rejects data quota expiry alerts and train PNR status messages.

### 3. 📊 Deep Visual Analytics & Drill-Downs
- **Interactive Daily Spending Trend Chart**: Custom Jetpack Compose Canvas bar chart showing daily expenditures with peak day indicators and tap-to-inspect tooltips.
- **Centered Category Breakdown & Distribution**: Centered interactive donut ring with center hub inspection, accompanied by a multi-segment horizontal proportional bar and 2-column percentage grid.
- **Payment Channels Split Card**: Visual split comparing **Credit Card** vs **Bank / UPI** expenditure.
- **Category Detail Bottom Sheet**: Tap any category to inspect total spent, % of monthly budget, average spend per transaction, top category merchants, and full transaction history.
- **Merchant Intelligence Sheet**: Tap any merchant in the leaderboard (e.g. `Blinkit`, `Swiggy`, `Zepto`) to view lifetime spend, order frequency, average ticket size, and historical orders.
- **Account Ledgers**: Tap any linked bank account or credit card to inspect total debits, total credits, and account-specific ledger records.

### 4. 👁️ Privacy & Balance Masking
- **100% Local On-Device Processing**: No cloud servers, no analytics trackers, no internet permissions required for parsing.
- **One-Tap Privacy Eye Toggle**: Mask and unmask all primary balances and account cards (`₹ ••••••••`) for privacy in public spaces.

---

## 🏗️ Architecture & Project Structure

```
Expense Manager/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/expensemanager/app/
│   │   │   │   ├── core/
│   │   │   │   │   ├── model/         # Domain models (Transaction, Account, Category, etc.)
│   │   │   │   │   └── util/          # Currency & DateTime formatters
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/         # Room Database, DAOs, and Entities
│   │   │   │   │   └── repository/    # SmsRepository, TransactionRepository, AccountRepository
│   │   │   │   ├── parser/            # SmsParser, SmsClassifier, CategoryClassifier, BankPatterns, DeduplicationEngine
│   │   │   │   ├── receiver/          # SmsBroadcastReceiver (Real-time incoming SMS listener)
│   │   │   │   └── ui/
│   │   │   │       ├── components/    # Apple-style Glass cards, DonutChart, DailySpendingChart, Sheets
│   │   │   │       ├── navigation/    # AppNavigation & Screen routes
│   │   │   │       ├── screens/       # Dashboard, Transactions, Analytics, Accounts, Settings
│   │   │   │       └── theme/         # Color palettes, Typography, Apple light elevations & shapes
│   │   │   ├── res/                   # Drawables, layouts, and strings
│   │   │   └── AndroidManifest.xml
│   │   └── test/                      # 25+ Comprehensive Unit Test Suites
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 2.0.0
- **UI Framework**: Jetpack Compose (BOM 2024.06.00) + Material 3
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Async & Reactive**: Kotlin Coroutines & StateFlow / SharedFlow
- **Local Storage**: Android Jetpack Room DB 2.6.1 with KSP
- **Charts & Graphics**: Jetpack Compose Canvas (Hardware-Accelerated)

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 34 / Build Tools 34.0.0)

### 1. Build and Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 2. Assemble Debug APK
```bash
./gradlew assembleDebug
```

### 3. Deploy via Wireless ADB
```bash
adb connect <DEVICE_IP>:<PORT>
adb -s <DEVICE_IP>:<PORT> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <DEVICE_IP>:<PORT> shell am start -n com.expensemanager.app/.MainActivity
```

---

## 🔒 Privacy Guarantee
This application operates **entirely offline**. All SMS messages, financial categorizations, bank accounts, and merchant data are parsed locally using regular expressions and domain heuristics, and stored exclusively in a secure on-device SQLite database via Android Jetpack Room.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
