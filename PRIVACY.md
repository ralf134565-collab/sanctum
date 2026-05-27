# Privacy Policy for Sanctum 🍃

**Effective Date: May 27, 2026**

We at the **Sanctum** project (hereinafter referred to as "the Application", "Sanctum", "we", "our", or "us") believe that your thoughts, reflections, and emotions belong exclusively to you. Our Application is built entirely on a **Local-First architecture**: we have no servers, no telemetry, no tracking, and no cloud databases.

Please read below to understand how your privacy and security are guaranteed when using Sanctum.

---

## 1. Data Collection and Transmission
* **We do not collect any personal data.** The Application does not require registration, accounts, emails, phone numbers, or social media linking.
* **The Application has no internet access.** The standard `android.permission.INTERNET` permission is physically stripped and blocked in the Application's manifest. Sanctum cannot transmit a single byte of data off your device.
* **No third-party sharing.** Because your data never leaves your device, sharing it with developers, advertisers, analytics platforms, or government authorities is technically impossible on an architectural level.

---

## 2. Cryptographic Local Storage
All your entries, mood tags, chat history with local AI, and personal anchors are stored strictly within the secure Android application sandbox:
* **Database Encryption (SQLCipher):** The local SQLite database is encrypted with military-grade **AES-256** encryption using 200,000 PBKDF2 iterations.
* **Hardware-Backed Keys (Android KeyStore):** Decryption keys are hardware-generated and stored in your device's secure environment (TEE/StrongBox).
* **Zero-Memory Footprint:** The Application explicitly zeroes out temporary passphrases in system memory (RAM) immediately after decrypting the database to prevent memory-dump attacks.

---

## 3. On-Device AI Privacy (Gemma)
All AI-mentor services, weekly summaries ("Weekly Mirror"), and persona chat features operate **100% autonomously on-device**:
* Processing is performed locally on your phone's processor using LiteRT (TensorFlow Lite).
* Your texts and conversations are never sent to external APIs (such as OpenAI, Google Cloud, etc.). No one, including the developers of Sanctum, can read your conversations with the local AI.

---

## 4. Device Permissions and Usage
Sanctum requests a minimal set of device permissions, each used exclusively for local, user-initiated features:
* **Biometric Authentication (Biometric API):** Used solely to verify your identity when unlocking the application (protecting your diary from snooping). The Application never sees or stores your fingerprint or face data — verification is handled by the Android OS, which only returns a success/failure status.
* **Vibration (VIBRATE):** Used to provide haptic feedback during UI interactions and the breathing ritual.
* **Run at Startup (RECEIVE_BOOT_COMPLETED):** Used by Android's WorkManager to warm up the local AI model in background RAM after a phone reboot, ensuring the app opens without delay.

---

## 5. Backups and Cloud Sync
* **Cloud Backups Disabled:** We explicitly disable automatic Google Drive backups (`allowBackup="false"`) in the manifest. This guarantees your encrypted database file is never uploaded to your personal Google Cloud storage without your conscious action.
* **Manual Local Export:** Backups are strictly manual and offline. You can export an encrypted backup file to your local storage and transfer it to a secure location of your choice. The backup password is created by you and is never stored in the Application.

---

## 6. Data Deletion
You are in absolute control of your data. You can completely and permanently delete all information at any time by:
1. Opening Sanctum Settings and selecting "Factory Reset".
2. Navigating to Android Settings -> Apps -> Sanctum -> Storage -> "Clear Storage / Clear Data".
3. Uninstalling the Application from your device.

---

## 7. Changes to This Privacy Policy
We may update this policy occasionally as we add new local, offline features. The latest version will always be available at:  
`https://github.com/ralf134565-collab/sanctum/blob/main/PRIVACY.md`

---

## 8. Contact Us
If you have any questions regarding the cryptographic security or privacy of Sanctum, contact us directly:
* **Email:** ralf.134565@gmail.com
* **GitHub:** [https://github.com/ralf134565-collab/sanctum](https://github.com/ralf134565-collab/sanctum)
