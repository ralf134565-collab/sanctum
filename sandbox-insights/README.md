# Insights Sandbox

Отдельное приложение для прототипа вкладки **«Картина»** без Room, Hilt и Gemma.

## Запуск

1. В Android Studio: Run Configuration → **sandbox-insights**.
2. На устройстве/эмуляторе откроется экран с переключателем **фикстур** (chips вверху).

## Фикстуры

| Chip | Что проверяет |
|------|----------------|
| Один тег / день | `MapPolygonMode.Hidden`, сектора полярности, карточки streak/dominant |
| Два тега, связки | Co-occurrence, полный/упрощённый полигон |
| Тревога → усталость | Sequence + recovery |
| Мало записей | Заглушки |
| Без повторов | Пустые закономерности |
| 10 вечеров | Preview-карта |
| 30 вечеров | «Золотой» сценарий |

Переключатель **30 / 90** и **EN** — на основном экране. Debug-строка под chips: `polygon`, `activeTags`, `avgTags`, `cards`.

## Модули

- `feature-insights` — domain + Compose UI (переиспользуется в Sanctum `app` позже).
- `sandbox-insights` — только fixtures + этот Activity.

## Тесты

```bash
./gradlew :feature-insights:testDebugUnitTest
```
