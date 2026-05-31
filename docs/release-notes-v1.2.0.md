# Sanctum 1.2.0

**Version:** 1.2.0 (versionCode 3)  
**Date:** 2026-05-31  
**GitHub Release:** [v1.2.0](https://github.com/ralf134565-collab/sanctum/releases/tag/v1.2.0)

## Где скачать

| Канал | Flavor | applicationId | APK |
|-------|--------|---------------|-----|
| **GitHub Releases** | `global` | `com.pocketreflect.app` | `app-global-release.apk` (прикреплён к релизу) |
| **RuStore** | `rustore` | `com.pocketreflect.app.rustore` | публикуется в магазине отдельно |

Это **два разных приложения** для Android: можно установить оба, данные между ними не синхронизируются.

- **global** — ссылка на поддержку ведёт на [DONATE.md](../DONATE.md)
- **rustore** — встроенная ссылка CloudTips (только в RuStore-сборке)

## Что нового

- **Песочный поток** — интерактивная релаксация на экране «Сегодня»: вращайте кольца, направляйте песок в центр
- Настройки в «Вечерний ритуал»: вкл/выкл, синхронизация с дыханием, три уровня сложности
- Опциональный прогрев модели ИИ при запуске (Настройки → Модель)
- Новый модуль `feature-mandala` в исходниках

## Исправления

- Стабильная работа после долгого сворачивания (дневник, история, чат) без биометрии
- Исправлен вылет чата при отправке сообщения
- Auto-lock только при включённой биометрической блокировке

## Сборка из исходников

```bash
./gradlew :app:assembleGlobalRelease    # GitHub / sideload (com.pocketreflect.app)
./gradlew :app:assembleRustoreRelease   # RuStore (com.pocketreflect.app.rustore)
```

Выходные файлы:

- `app/build/outputs/apk/global/release/app-global-release.apk`
- `app/build/outputs/apk/rustore/release/app-rustore-release.apk`

Полный changelog: [CHANGELOG.md](../CHANGELOG.md)
