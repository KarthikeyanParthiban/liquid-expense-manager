# LQD

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=flat&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Repository%20%2B%20Flow-blue)](https://developer.android.com)
[![Storage](https://img.shields.io/badge/Storage-Room%20Database%20(100%25%20On--Device)-00599C?style=flat&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Release](https://img.shields.io/badge/Release-v1.2.0-emerald.svg)](https://github.com/KarthikeyanParthiban/liquid-expense-manager/releases/latest)

An intelligent, privacy-first, on-device money manager for Android that transforms transactional notifications and SMS messages from Indian banks, credit cards, and UPI apps into actionable financial analytics with a **sleek, fluid dark/light design system**.

---

## 📥 Download Latest APK

👉 **[Download LQD Latest APK](https://github.com/KarthikeyanParthiban/liquid-expense-manager/releases/latest)**

---

## ✨ Key Highlights & Features

### 1. 📊 7-Day Cash Flow Spectrum & Insights Monotone Palette
- **Last 7 Days Rolling Window**: Live daily comparison of Credit (Income) and Debit (Spend) with average daily burn rate.
- **Apple Monotone Aesthetic**: Crisp White & Titanium Slate palettes with zero harsh colors.
- **1-Tap Deep Dive**: Tapping the chart opens deep analytics and merchant breakdowns.

### 2. 🔍 Unified Search & Filter Bottom Sheet
- **Streamlined Quick Bar**: 1-row type selector (`All`, `Debits`, `Credits`, `Refunds`) with active filter tags.
- **Dedicated Filter & Sort Modal**: Organize transactions by *Newest*, *Oldest*, *Highest*, *Lowest Amount*, and *15+ Categories* without screen clutter.

### 3. 🧠 Smart SMS Parser & Deduplication Engine
- **Multi-Bank Reconciliation**: Resolves simultaneous alerts (Bank SMS + UPI app push) into a single consolidated transaction.
- **Bill Due vs Payment**: Distinguishes statement dues from executed debits to eliminate double counting.
- **Spam & Ad Rejection**: Automatically excludes non-transactional marketing messages, loan clickbaits, demat summaries, and OTPs.

### 4. 🔒 100% Privacy & Offline-First Security
- **Local On-Device Parsing**: All SMS parsing and categorization happens locally on your device in real-time.
- **Zero Cloud Leakage**: No financial transaction data leaves your phone.
- **Balance Masking**: 1-tap eye icon to mask your balances in public spaces (`₹ ••••••••`).

---

## 🏗️ Tech Stack

- **Framework**: Jetpack Compose + Material 3
- **Language**: Kotlin 2.1.0 (Coroutines + Flow)
- **Local DB**: Android Jetpack Room DB 2.6.1 + KSP
- **Charts**: Hardware-Accelerated Jetpack Compose Canvas
- **Background Sync**: Android WorkManager & BroadcastReceiver

---

## 🛠️ Developer Setup & Build

### Run Unit Tests
```bash
./gradlew test
```

### Build Release APK
```bash
./gradlew assembleRelease
```

### Deploy to Device via ADB
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.expensemanager.app/.MainActivity
```

---

## 📄 License
This project is licensed under the MIT License.
