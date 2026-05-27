## Summary
<!-- Provide a concise 1-2 sentence description of your changes and why they are necessary. -->

## Changes Checklist
- [ ] Added/updated relevant unit or integration tests.
- [ ] Localized all new UI strings in both English (`strings.xml`) and Russian (`strings-ru.xml`).
- [ ] Verified local compilation and built the project successfully.

## 🛡️ Privacy & Cryptographic Safety Verification (MANDATORY)
*To maintain Sanctum's strict security guarantees, every contributor MUST verify the following security checkpoints before merging:*

- [ ] **No Internet**: Verified that no internet-facing libraries, network requests, or network permissions have been introduced.
- [ ] **Zero-Memory Lifetimes**: Checked that any user-entered passwords are treated strictly as `CharArray` (or wrapped in `SecurePasswordState`) and explicitly wiped from memory with `Arrays.fill` or `clear()` immediately after use. No sensitive inputs exist as immutable `String` instances in the JVM String Pool.
- [ ] **Database Integrity**: Confirmed that any database-facing changes respect the active cryptographic biometric lock, close connections correctly upon locking, and do not bypass `AuthSessionHolder`.
- [ ] **JNI Resource Release**: Verified that any local AI-related resource handles (such as LiteRT engine buffers) are proactively released/reclaimed under cancellation or termination to prevent JNI memory leaks or thread hangs.
- [ ] **Prohibited Clichés**: Double-checked that no manipulative, patronizing, or cheap clinical/coaching clichés ("you're doing great", "don't worry", etc.) have been added to the local AI prompt instructions.
