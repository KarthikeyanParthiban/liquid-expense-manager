# Changelog

All notable changes to **LQD (Liquid Expense Manager)** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0.7] - 2026-08-31

### Added
- **Inactive / Dormant Account Detection**: Accounts and credit cards with no SMS activity for > 1 year (365 days) are automatically marked dormant with visual `INACTIVE` status tags.
- **Balance & Spend Calculation Exclusions**: Inactive accounts are excluded from total active bank balance and spending calculations so outdated balance snapshots do not distort liquid wealth metrics.
- **Last Received SMS Timestamp Display**: Denotes explicit `as of <date>` indicators on all card and list views, as well as full last received SMS timestamps (`as of DD MMM YYYY, hh:mm a`) and dormancy explanation callouts inside expanded account ledger sheets.

---

## [1.3.0.6] - 2026-08-31

### Added
- **Hierarchical Bank Identification Engine**: Explicit in-body user account ownership (`your HDFC Bank A/c...`, `Dear SBI User, your A/c...`, `spent on ICICI Card...`) now strictly overrides third-party PSP gateways (`YESBNK`, `AXISBK`, `ICICIB`, `SBIUPI`, `PAYTM`, `GPAY`, `CREDIN`), eliminating multi-bank account fragmentation.
- **Prioritized Owning Account Extractor**: Added 2-pass regex parsing to prioritize user source/owning accounts and suppress counterparty/beneficiary accounts in inter-account transfers and UPI payment notifications.
- **Cross-Bank Account Disambiguation & Self-Healing**: Dynamic resolution reconciles ambiguous gateway transactions to known verified issuing bank accounts. Database self-healing automatically consolidates multi-bank duplicate accounts and purges ghost cards.
- **TRAI Header Normalization**: Added comprehensive support for TRAI DLT headers (`XX-ENTITY-T` / `XX-ENTITY`) with expanded Indian banking codes (Bandhan Bank, DBS Bank, etc.).

---

## [1.3.0.5] - 2026-08-30

### Fixed
- **Q Letter Vector Geometry**: Corrected geometric vector paths for the letter **Q** across the app startup splash screen, Android vector drawables, React/HTML components, and brand logo exports. Fixed mismatched cutlines, eliminated open voids, and aligned the 45° diagonal tail with symmetrical 47px negative space gaps and 86px uniform stroke weight.

---

## [1.3.0.4] - 2026-08-30

### Added
- **Official Geometric Vector Logo**: Replaced text wordmark on the startup animation and across UI components with the official rebuilt geometric LQD vector logo (`1187×536` true geometry).
- **Minimalist Sync & Classification Redesign**: Completely overhauled the transaction classification & SMS sync overlay to eliminate neon and rainbow gradients in favor of an understated monochromatic, OLED-aligned design matching the LQD design system.
- **Unified Brand Loader**: Integrated the signature `LumaSpinLoader` and clean completion states across the sync pipeline.

---

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
