# Changelog

All notable changes to **LQD (Liquid Expense Manager)** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0.3] - 2026-08-29

### Added
- **Dynamic VPA vs Beneficiary Bank Parser**: Accurately strips third-party handle VPAs (`@okicici`, `@okaxis`, `@oksbi`, `@paytm`, `@ybl`, etc.) before identifying the underlying user account, eliminating misclassification of transfer beneficiary banks.
- **Telecom & Promo Noise Rejection**: Filters out mobile recharge confirmations, data pack validity notices, and promotional cashback pitches.
- **Strict Device Data Isolation**: Excluded chat notification listener to guarantee personal WhatsApp/chat messages never leak or get parsed across devices.
- **Muthoot Fincorp (`MUTFCL`) Support**: Added transaction parsing and loan classification for Muthoot Fincorp alerts.
- **Database Self-Healing & Orphan Pruning**: Automatic startup purge of legacy unmasked/primary accounts and noise records.
- **Liquid Full-Loop Startup Reveal**: Synchronized Luma Spin orbital animation with full 360° loop completion and smooth neutral dark-mode gradient swipe-up reveal.

---

## [1.3.0.2] - 2026-08-27

### Added
- **Uncapped SMS Inbox Scanning**: Removed artificial 10,000 SMS scan limit in `SmsRepository`, enabling full historical scanning across 100% of device inbox messages.
- **Automated Release Notes System**: Build-time validation and Keep-a-Changelog extraction.

---

## [1.3.0.1] - 2026-08-27

### Added
- **Strict Account & Card Mask Requirements**: Accounts and Credit Cards are now only created and displayed when explicit ending digits / mask (`XX7011`, `XX1006`, `XX9117`, `XX2942`, `XX2643`, `XX9337`, `XX2129`, `XX2558`) are present in SMS or notifications.
- **Enhanced Card Number Matching**: Added support for hyphenated card formats (`XXXX-1006`), `Card X1006`, and Kiwi/CRED bill payment card resolution.
- **Database Query Filtering**: Filtered out legacy `PRIMARY` and unmasked cards from dashboard and ledger views.

### Fixed
- **UPI VPA Bank Disambiguation**: Resolved bug where recipient `@vpa` handles (`@okhdfcbank`, `@icici`, `@axisbank`, `@paytm`, `@ybl`, `@vpa`, etc.) were mistaken for user bank accounts.
- **Stat Card Typography Scaling**: Added dynamic font scaling (`12.5.sp` - `16.sp`) and single-line ellipsis safeguards in `GlassStatCard` to prevent number wrapping on large expense amounts (e.g. `₹12,85,893.27`).

---

## [1.3.0] - 2026-08-27

### Added
- **Official LQD Rebranding**: High-resolution vector-sharp adaptive launcher icons (`mdpi` through `xxxhdpi`) and 512×512 Play Store icon generated from official brand artwork.
- **Modern Startup Screen**: Minimalist wordmark (`L` and `D` obsidian/white, `Q` `#0055FF` Royal Electric Blue) with smooth swipe-up reveal.
- **In-App Seamless OTA Updater**: Liquid titanium glass update dialog with live download byte counters and clean bulleted release notes.

---

## [1.2.0] - 2026-08-26

### Added
- **Apple Monotone Cash Flow Analytics**: 7-day cash flow trend line with interactive scrub points and category breakdown.
- **Streamlined Transactions Filter & Sorting Sheet**: Single-strip bottom sheet for filtering transactions by bank, category, and date range.
- **Multi-Currency Engine**: Full support for international currencies (USD, EUR, GBP, AED, SGD, CAD, JPY, INR).

---

## [1.1.1] - 2026-08-26

### Added
- **Uninterrupted Background SMS Sync**: ForegroundService, WakeLock, and WorkManager periodic synchronization for real-time reconciliation.
- **Dynamic Semver Version Detection**: Automatic runtime comparison against GitHub releases.

---

## [1.1.0] - 2026-08-26

### Added
- **3-Tier Transaction Classifier**: Regex context rules, UPI VPA token decomposition, and on-device ML fallback.
- **Account Ledger Bottom Sheet**: Detailed breakdown of account balances and past transactions per card.

---

## [1.0.0] - 2026-08-25

### Added
- **Initial Release**: Real-time SMS parsing, dark/light glassmorphic UI, and expense tracking dashboard.
