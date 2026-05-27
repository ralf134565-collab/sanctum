# Contributing to Sanctum 🍃

We are thrilled that you are interested in contributing to Sanctum! As an open-source, local-first wellness application, we welcome community improvements, translation updates, and optimizations.

---

## 🔒 Crucial Architectural Invariants

Before writing any code, please understand that Sanctum has **strict security and privacy invariants** that cannot be violated under any circumstances:

1. **Zero Network Calls**
   - Sanctum does **not** declare the `android.permission.INTERNET` permission in its production manifest.
   - You must never write code that attempts to fetch or send data over a network or call external APIs in production.

2. **Secure Passphrase Lifecycle**
   - User-entered passwords must never exist in JVM heap memory as immutable `String` instances.
   - Always use `SecurePasswordState` and `CharArray` for password inputs.
   - Always wrap password operations in `try-finally` blocks and call `clear()` or `Arrays.fill(..., '\u0000')` to zero out the memory immediately after use.

3. **Active Database Isolation**
   - Active Room/SQLCipher database connections must be closed when the app goes to the background.
   - If you modify database-facing screens or ViewModels, ensure they respect the biometric lock and do not bypass `AuthSessionHolder` security.

---

## 🛠️ How to Contribute

### 1. Preparing Your Environment
- Install Android Studio Ladybug (or newer).
- Set up Android SDK 34+.
- Ensure you are using JDK 17.

### 2. Branching & PR Guidelines
- Create a new branch from `main` for your changes.
- Ensure your changes are focused and modular.
- Write unit tests for new logic where possible.
- Run local linting and compilation to verify your changes build cleanly.

### 3. Commit Message Style
We prefer concise, meaningful commit messages that explain **why** the change was made:
- `fix: secure password input erasure in recovery dialog`
- `feat: add german localization for settings screens`
- `refactor: optimize local-AI engine memory buffer release`

---

## ⚖️ Contributor License Agreement (CLA)

By contributing to Sanctum, you agree that:
1. Your code contributions (Kotlin, XML, Gradle, configuration files, documentation) will be licensed under the **GNU General Public License v3.0 or later** (GPL-3.0-or-later).
2. Any contributions to the creative text corpora, mock response matrices, few-shot examples, or daily reflection prompts will belong to the project copyright holder (**Viacheslav Zhukov**) to preserve unified brand integrity and ensure prompt protection.

If you have any questions or need to reach out privately, contact us at: ralf.134565@gmail.com
