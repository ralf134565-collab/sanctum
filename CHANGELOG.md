# Changelog

All notable changes to Sanctum (PocketReflect) are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.3.0] - 2026-05-31

### Added

- **Insights** tab (`Картина`) — local summary of mood labels and patterns for 30/90 days (`feature-insights`)
- **Evening music** — ExoPlayer ambience while the app is in foreground; 5 built-in Ogg tracks + up to 5 user MP3/Ogg imports
- **Custom chat persona** (`Свой стиль`) — user style prompt with fixed Sanctum safety kernel; separate chat history
- Settings hub: Chat section; ritual settings grouped under “On the Today tab”
- Rename custom ambient tracks on import; import tip for instrumental / nature sounds
- Philosophy & methodology dialog updated (RU/EN) for music, Sand Flow, Insights, custom chat

### Changed

- App version: **1.3.0** (versionCode **4**)
- History month header: top **1** mood tag instead of top 3
- Bottom navigation: equal-width tabs, shorter labels (fixes “Settings” truncation)
- Russian UI: “Sanctum” → “Санктум” where shown to users

### Fixed

- Ambient music no longer restarts from the beginning when adjusting volume
- Music starts paused until Play on Today; no auto-play on app foreground

## [1.2.0] - 2026-05-31

### Added

- **Sand Flow** (`Песочный поток`) — interactive kinetic relaxation ritual on the Today tab
- Settings in Evening Ritual: enable/disable, breathing sync, three difficulty levels
- `feature-mandala` Gradle module (mandala engine and canvas UI)
- Optional AI model warmup on launch (Settings → Model)
- Unified `DatabaseAccess` layer for encrypted Room database
- Unit tests for auth session and database access lifecycle

### Fixed

- App became unusable after long background when biometric lock was disabled (journal error, history spinner, chat crash)
- Chat no longer crashes on send when the database was temporarily unavailable
- Auto-lock timeout now applies only when biometric lock is enabled (matches Settings UI)
- Biometric lock toggle correctly locks the app when enabled from Settings

### Changed

- App version: **1.2.0** (versionCode **3**)
- Updated launcher icons (adaptive icons)
- Privacy policy: clarified RAM passphrase wiping applies when biometric lock is enabled
- Manual QA checklist: long-background test cases

## [1.0.0] - 2026-05-27

### Added

- Initial open-source release of Sanctum
- Local-first encrypted journal (SQLCipher + Room)
- On-device AI mentor and chat (Gemma / LiteRT-LM)
- Biometric gate, backup/vault export, history search
- RuStore flavor with donation links (CloudTips)

[1.3.0]: https://github.com/ralf134565-collab/sanctum/releases/tag/v1.3.0
[1.2.0]: https://github.com/ralf134565-collab/sanctum/releases/tag/v1.2.0
[1.0.0]: https://github.com/ralf134565-collab/sanctum/commit/4a32077
