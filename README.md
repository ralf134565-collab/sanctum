# Sanctum 🍃

Sanctum is a beautifully designed, local-first, privacy-respecting mental wellness and daily reflection application for Android.

Every aspect of Sanctum is architected to protect your inner world. Unlike typical wellness apps, your reflections, feelings, and goals never touch the cloud. All insights, mentoring, and summaries are generated **directly on your device** using local, highly-optimized on-device AI.

---

## 🔑 Core Pillars & Security Architecture

1. **Local-First & Offline-by-Default**
   - No internet permission is declared in the `AndroidManifest.xml` (except standard biometric APIs).
   - Zero servers, zero telemetry, zero analytics tracking.

2. **Cryptographic Storage & Hardware Protection**
   - **SQLCipher with Room**: The local SQLite database is cryptographically encrypted using SQLCipher with **200,000 PBKDF2 iterations**.
   - **Android KeyStore Integration**: Database decryption keys are generated and managed inside the secure, hardware-backed Android KeyStore (TEE/StrongBox) with **AES-GCM (256-bit)**.
   - **Zero-Memory Footprint**: All entered passphrases use `CharArray` and are explicitly zeroed out in RAM using memory wiping (`Arrays.fill`) immediately after use.

3. **Strict Biometric Gate**
   - When biometric lock is enabled, the database is closed and passphrases are wiped from RAM when the app is backgrounded until you unlock again.
   - WorkManager background processes defer work while the database is locked.

4. **Evening Rituals**
   - **Breathing bridge** — guided resonant or box breathing before reflection.
   - **Sand Flow** — interactive kinetic relaxation (rotate rings, guide sand into the core).

5. **Private On-Device AI (LiteRT-LM)**
   - All reflection mentoring is processed using local **Gemma** quantized models running via the Google LiteRT (TensorFlow Lite) runtime.
   - Streaming is thread-safe and isolated. In case of process memory trim or cancellation, resources are proactively reclaimed to prevent JNI lockups.

---

## 🛠️ Tech Stack & Requirements

- **Platform**: Android 9.0+ (API 28+)
- **Minimum RAM**: 6 GB (for Gemma E2B variant), 8 GB (for Gemma E4B variant)
- **UI Framework**: Jetpack Compose (Material 3, fully localized and dynamic theming)
- **Dependency Injection**: Hilt / Dagger
- **Database**: Room Persistence Library with Zetetic SQLCipher
- **Background Work**: Android WorkManager
- **On-Device LLM**: LiteRT-LM (Gemma E2B/E4B variants)

---

## 📂 Project Structure

```
├── app/                    # Main Sanctum application
├── feature-mandala/        # Sand Flow engine and canvas (used by app)
└── sandbox-mandala/        # Standalone mandala dev sandbox (optional)
```

---

## 🚀 Getting Started & Local Setup

### 1. Requirements & Tools
- Android Studio Ladybug (or newer)
- Android SDK 34+
- JDK 17

### 2. Project Compilation
Clone the repository and open it in Android Studio:
```bash
git clone https://github.com/ralf134565-collab/sanctum.git
```

To configure release APK signing:
1. Create a `release-signing.properties` file in the project root directory (this file is pre-configured in `.gitignore`).
2. Populate it with your keystore path and credentials (see template `release-signing.properties.template`):
```properties
storeFile=release.jks
storePassword=your_keystore_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

---

## 🎁 Support the Project & Donations

Sanctum is developed entirely as a labor of love for human privacy and digital wellness. We have a strict **no in-app donation blocks** policy to keep your reflection space peaceful and distraction-free.

If you believe in our mission of offline, private mental health software, please consider supporting development:

- Star our repository on GitHub!
- See [CHANGELOG.md](CHANGELOG.md) for release history.

---

## 📋 Releases

Tagged releases: [GitHub Releases](https://github.com/ralf134565-collab/sanctum/releases).  
Latest: **1.2.0** — Sand Flow, stability after background, database access improvements.

Build RuStore APK: `./gradlew :app:assembleRustoreRelease`  
Build global APK: `./gradlew :app:assembleGlobalRelease`

---

## 🛡️ Responsible Disclosure & Security Policy

Privacy is our absolute promise. If you discover any security issues, vulnerabilities, or potential side-channel leaks, please do **not** open a public issue. Instead, please follow the guidelines in our [SECURITY.md](SECURITY.md) to submit a report privately.

---

## 📄 License & Dual-Licensing

This repository contains two distinct licensing tracks:

1. **Source Code**:
   Licensed under the **GNU General Public License v3.0 or later** (GPL-3.0-or-later). See the [LICENSE](LICENSE) or [COPYING](COPYING) file for full details.

2. **Creative Content & Prompt Corpora**:
   The contents of all prompt text corpora, few-shot examples, response pool matrices, and Daily Prompts (`DailyPrompts.kt`, `DailyPromptsEn.kt`, `JournalPrompts.kt`, `JournalPromptsEn.kt`, `ChatPrompts.kt`, `ChatPromptsEn.kt`, and `MockGemmaTextPools.kt`) are protected separately as creative literary works and are **strictly dual-licensed**. See the respective file headers and [TRADEMARK.md](TRADEMARK.md) for usage restrictions.
