# Mini Accountant / حسابدار کوچک

Personal daily ledger for Iranian users. Persian UI, RTL, Vazirmatn, amounts in **Long rials** (optional Toman display).

Package: `ir.mhajisoft.miniaccountant`

## Open in Android Studio

1. Install Android Studio (SDK **compile 37**, **target 36**, **min 26**).
2. Copy `local.properties.example` → `local.properties` and set `sdk.dir`.
3. Open this folder. Sync Gradle. Run the `app` debug configuration.
4. JDK 17+.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Features

- Onboarding: fiscal year (default 1 Farvardin, Birashk leap Esfand), first cash account, optional device lock
- Home / transactions / Jalali reports (Vico) / more
- Accounts, people wallets, system categories (Material Symbols)
- Transfers (atomic two legs + optional fee «کارمزد»)
- Card BIN logos + Iranian IBAN (Sheba) validation
- Fiscal year opening balances, archive of closed years, SAF backup

## CVV / backup policy

- Default: **do not persist CVV2**. Optional per-card remember uses Tink AES-256-GCM + Android Keystore with user authentication (`BiometricPrompt` CryptoObject).
- PAN is masked except last 4. Reveal requires device credential.
- Backups set `includesSecrets=false`. CVV ciphertext is stripped from the zip. Never log PAN/CVV.
- Clipboard CVV is cleared after 30 seconds. Vault screens use `FLAG_SECURE`.

## Google Play services / Cafe Bazaar

The **core ledger works with zero Google Play services** (Iran / Cafe Bazaar builds).

- **Local SAF backup always works.**
- **Google Drive** (`drive.appdata`) is hidden or disabled when `GoogleApiAvailability` reports no GMS, with Persian copy.
- **OneDrive** (MSAL + Graph `approot`) stays disabled until `ONEDRIVE_CLIENT_ID` is set in `local.properties`. The app never fakes a successful cloud upload.

## Stack

Kotlin 2.3 (K2), Compose Material 3, Hilt + KSP, Navigation 3, Room 3.0.2 + BundledSQLiteDriver, Vico 3, jalalidate (Birashk / 33-year civil leap cycle), Tink, androidx.biometric.

## License

MIT. See [LICENSE](LICENSE).
