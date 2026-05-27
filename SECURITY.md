# Security Policy 🛡️

We take the privacy and cryptographic security of Sanctum extremely seriously. Since Sanctum is designed as a secure, offline, local-first journal, we hold ourselves to the highest technical standards of data protection.

---

## 🔒 Supported Versions

Only the latest release of Sanctum is supported with security updates and patches. Please ensure you are running the most recent version available.

| Version | Supported |
| ------- | --------- |
| 1.0.x   | ✅ Yes     |
| < 1.0   | ❌ No      |

---

## 🔍 Reporting a Vulnerability

If you discover a security vulnerability, side-channel leak, or cryptographic weakness, we request that you **do not** open a public issue. Doing so puts user data at risk.

Instead, please report the vulnerability privately by emailing us at `ralf.134565@gmail.com`.

### What to Include
To help us investigate and patch the issue as quickly as possible, please provide:
- A clear description of the vulnerability or side-channel leak.
- Detailed step-by-step instructions or proof-of-concept (PoC) code to reproduce it.
- Your assessment of the impact (e.g., local key extraction, memory leak, sandbox bypass).

### Our Commitment
If you report a vulnerability privately and responsibly:
- We will acknowledge receipt of your report within **48 hours**.
- We will work closely with you to validate and understand the issue.
- We will coordinate a patch and release a security update in a timely manner.
- We will credit you for the discovery in our release notes (if you wish to be named).

---

## 🎯 Threat Model

Sanctum is built with an offline-first, local-only architecture. Because of this, our security guarantees depend heavily on the device status and specific attack vectors:

### In Scope
- **Casual snooping / family member access**: A person picking up your unlocked device cannot read your journal or chats, as they are gated by the biometric lock screen.
- **Lost or stolen device (casual thief)**: If your phone is lost or stolen without root access enabled, the local database is secure. The SQLite database is encrypted with AES-256 (via SQLCipher) using a dynamically generated key.
- **Non-root access**: Other apps running on the same device cannot access Sanctum's database or keys due to Android's strict application sandboxing.

### Out of Scope / Intentional Trade-offs
- **Forensic attacker with root + physical access**: If an attacker gains full root access and physical access to the device, they can extract the master key directly from the Android KeyStore using standard reverse-engineering tools (since any process running under Sanctum's UID can request decryption).
- **Why we don't bind the key to biometric authentication**: We do not use `.setUserAuthenticationRequired(true)` or `.setUserAuthenticationParameters(...)` in our `SecureKeyManager` key generation. Binding the hardware key directly to biometrics means that if the user changes, resets, or registers a new fingerprint, Android invalidates the key. For a local-only app, this would mean **permanent and irreversible loss of all journal entries and chats**, which is a worse UX failure than the local root vulnerability.
- **Summary**: The biometric gate acts as a highly effective runtime lock that closes the database and wipes the passphrase from RAM when the app goes into the background, but the master encryption key stored in the Android KeyStore is not cryptographically bound to biometric authentication. This is an intentional trade-off to ensure your memories are never permanently locked away.
