# Sanctum — вкладка «Картина» (Insights): финальная спецификация

Версия: 1.1  
Статус: **прототип в sandbox** — см. `feature-insights`, `sandbox-insights`, [sandbox-insights/README.md](../sandbox-insights/README.md)  
Связанные сущности: `JournalEntry`, `MoodTag`, `GemmaLocalEngine`, `EngineCoordinator`

---

## 1. Резюме

**«Картина»** — пятая вкладка в нижнем меню. Локальный обзор вечерних записей за выбранный период:

1. **Сводка отметок** — кольца полярности + 10-угольник по фиксированным mood-тегам (не «оценка», не «норма»).
2. **Закономерности** — до 7 карточек с правилами «в N из M вечеров…».
3. *(Фаза 4)* Опционально — бережное пояснение от Gemma **по кнопке**.

Пользователь **ничего дополнительно не заполняет**: только уже сохранённые теги и тексты.

**Цель для человека:** один спокойный визит → узнавание («так, вот что повторялось») → тап к конкретным дням. Не отчёт, не диагноз, не сравнение с идеалом.

**Цель для продукта:** отдельный домен `insights` без перегруза «Истории»; задел под будущие обзоры (год, сравнение периодов) без второй ML-модели.

---

## 2. Принципы (инварианты)

### 2.1. Совместимость с «без оценок»

| Делаем | Не делаем |
|--------|-----------|
| «Вы **отмечали** тег X в N вечеров» | «Низкий уровень спокойствия» |
| «**Чаще** встречались A и B» | «Дисбаланс», «плохой месяц», «нужно улучшить» |
| Период: «за 30 дней» | «Ваш профиль / тип личности» |
| Нейтральные цвета, без красных «алертов» | Стрелки ↓ «стало хуже», эталонный «идеальный» контур |
| Статистика **ваших** отметок | Сравнение с другими пользователями или «нормой» |

**Формулировка для UI (под заголовком вкладки, `bodySmall`):**

- RU: *«Сводка ваших отметок за период. Это не оценка и не совет врача.»*
- EN: *«A summary of your labels for this period. Not a score, not medical advice.»*

**Disclaimer (один раз внизу экрана, collapsible после первого прочтения — опционально v1.1):**

- RU: *«Закономерности считаются автоматически по записям на устройстве. При кризисе опирайтесь на людей и профессиональную помощь.»*

### 2.2. Local-First

- Без сети, без новой модели, без фонового Worker для индексации.
- Пересчёт при открытии вкладки (и при смене 30/90) на `Dispatchers.Default`.
- Опциональный кэш Room — фаза 1.1+, не блокер MVP.

### 2.3. Инсайт, не отчёт

Успех экрана: пользователь может своими словами сказать *«не знал, что так часто связаны X и Y»* или *«вижу, что месяц был в основном про усталость»* — без чувства, что приложение вынесло вердикт.

---

## 3. Навигация и информационная архитектура

### 3.1. Нижнее меню (5 вкладок)

| Порядок | Route | RU | EN | Иконка (предложение) |
|---------|-------|----|----|----------------------|
| 1 | `today` | Сегодня | Today | Edit |
| 2 | `history` | История | History | History |
| 3 | **`insights`** | **Картина** | **Insights** | **AutoGraph** |
| 4 | `chat` | Чат | Chat | Forum |
| 5 | `settings` | Настройки | Settings | Settings |

`EntryDetail` остаётся глобальным push: `entry/{id}` (как из Истории).

### 3.2. Разделение ролей вкладок

| Вкладка | Вопрос пользователя |
|---------|---------------------|
| Сегодня | Что записать сегодня? |
| История | Что было в конкретный день? |
| **Картина** | **Как складывались вечера за месяц/квартал?** |
| Чат | Поговорить с ментором |
| Настройки | Приватность, ритуал, модель |

### 3.3. Точка входа с «Сегодня» (фаза 3)

**Баннер** (не модалка), условия показа:

- `entriesInLast30Days >= 12`
- пользователь ещё не открывал вкладку «Картина» **или** прошло ≥ 14 дней с последнего визита
- не чаще 1 раза в 14 дней
- не показывать в активном поиске / не в short ritual save flow

Текст RU: *«Накопилось достаточно вечеров — посмотрите сводку отметок и закономерности»*  
CTA: *«Открыть картину»* → `navController.navigate(Routes.Insights)`

DataStore: `insights_banner_last_shown_ms`, `insights_tab_ever_opened`.

---

## 4. Экран «Картина» — компоновка (все фазы)

```
┌─────────────────────────────────────────────────┐
│ TopAppBar: Картина                              │
│ subtitle: Сводка отметок · не оценка            │
│                              [ 30 дней │ 90 ]   │  ← фаза 3: переключатель
├─────────────────────────────────────────────────┤
│ § A. СВОДКА ОТМЕТОК (StateMap)                 │  ← фаза 2
│   [Polarity rings + 10-gon + legend]            │
│   caption: auto-generated, 1 line             │
├─────────────────────────────────────────────────┤
│ § B. ЗАКОНОМЕРНОСТИ (Pattern cards)            │  ← фаза 1
│   card × up to 3 visible                        │
│   [ Показать ещё ] → up to 7                    │
├─────────────────────────────────────────────────┤
│ § C. (фаза 4) Gemma block — only if attached    │
│   collapsed by default                          │
├─────────────────────────────────────────────────┤
│ footer disclaimer                             │
└─────────────────────────────────────────────────┘
```

Скролл один, вертикальный, фон `screenAtmosphereGradient` как в Истории.

---

## 5. Фаза 1 — Вкладка + карточки + пороги

### 5.1. Scope

- `Routes.Insights`, `TopLevelDestination.INSIGHTS`, `InsightsScreen`, `InsightsViewModel`, `InsightsContract`.
- `domain/insights/*` — детектор, форматтер, policy.
- Карточки типов **1, 2, 3, 5** (см. §7).
- Заглушки по порогам (§6).
- Период **только 30 дней**.
- Sheet «дни паттерна» → `EntryDetail`.
- RU + EN strings.

**Вне scope фазы 1:** карта (§8), 90 дней, баннер, Gemma, подсветка карта↔карточки.

### 5.2. Состояния § B (закономерности)

| ID | Условие | UI |
|----|---------|-----|
| `INSUFFICIENT_DATA` | `entriesInWindow < 12` | Одна карточка-заглушка (см. copy §12) |
| `NO_PATTERNS` | ≥12 записей, 0 карточек прошли пороги | Заглушка «явных повторов пока нет» |
| `HAS_PATTERNS` | ≥1 карточка | До 3 видимых + «Показать ещё» до 7 |

### 5.3. Карточка — UI

- `InsightPatternCard`: icon by type, title, body, evidence line.
- Tap → `InsightPatternSheet`: список `dayBucket` + теги; row tap → `NavigateToDetail(entryId)`.

---

## 6. Пороги и политика (`InsightPolicy`)

```kotlin
object InsightPolicy {
    const val WINDOW_30_DAYS = 30
    const val WINDOW_90_DAYS = 90          // фаза 3
    const val MIN_ENTRIES_FULL = 12        // карточки + полная карта
    const val MIN_ENTRIES_PREVIEW = 8      // фаза 2: бледная карта + «предварительно»
    const val MIN_PATTERN_SUPPORT = 4
    const val MIN_PATTERN_RATE = 0.55
    const val MAX_CARDS = 7
    const val MAX_KEYWORD_CARDS = 0        // тип 6 — post-MVP
    const val VISIBLE_CARDS_COLLAPSED = 3
}
```

Окно: записи с `dayBucket` в `[today - N days, today]` (локальная TZ, как везде в приложении).

---

## 7. Закономерности — типы и правила

Общий вход: `List<JournalEntry>` sorted by `dayBucket` asc.  
Выход: `List<InsightPattern>` с `id`, `type`, `support`, `base`, `entryIds`, `score`.

### Тип 1 — `CO_OCCURRENCE` (в один вечер)

- Теги A ≠ B в `moodTags` одной записи.
- `support >= MIN_PATTERN_SUPPORT`
- `support / count(entries containing A) >= MIN_PATTERN_RATE`
- Перебор пар; приоритет score: негатив+нейтрал, негатив+негатив, негатив+позитив, остальное.

**Copy RU:**  
Title: `{A} и {B} часто в один вечер`  
Body: `В {support} из {base} вечеров с «{A}» вы также отмечали «{B}».`

### Тип 2 — `SEQUENCE` (следующий календарный день)

- Запись D с A; существует запись на D+1 с B (день без записи разрывает цепочку для этой пары).
- База: вечера с A, у которых есть запись на D+1.
- Пороги как тип 1.
- Приоритетные пары в scorer: `ANXIETY→TIRED`, `OVERWHELMED→TIRED`, `IRRITATION→ANXIETY`, негатив→`CALM`.

**Copy RU:**  
Title: `После «{A}» на следующий вечер часто «{B}»`  
Body: `Так было в {support} из {base} раз.`

### Тип 3 — `STREAK` (подряд по записям)

- ≥3 **последних** записи в окне (по дате), каждая содержит тег T.
- Максимум **1** карточка streak: самая длинная; при равенстве приоритет негатив.

**Copy RU:**  
Title: `Несколько вечеров подряд с «{T}»`  
Body: `В {support} последних записях подряд (с {from} по {to}).`

### Тип 5 — `RECOVERY_MICRO_WINS` (осторожный позитив)

- D: `moodTags.hasNegative`; D+1: запись есть, `microWins.isNotBlank()`, на D+1 нет негативных тегов.
- База: все пары (D,D+1) с негативом на D.
- Пороги как выше.
- **Не** показывать обратную карточку «редко победы после тяжёлого».

**Copy RU:**  
Title: `После тяжёлого вечера иногда находили опору`  
Body: `В {support} из {base} раз на следующий день были заполнены микро-победы.`

### Тип 4 — `DOMINANT_TAG` (низкий приоритет)

- Тег T в ≥45% всех записей окна и `support >= MIN_PATTERN_SUPPORT`.
- Показывать только если не набралось 3+ карточек типов 1–3, 5.

### Ранжирование

`score = support * typeWeight`; dedupe пересекающихся пар; top `MAX_CARDS`.

---

## 8. Фаза 2 — Сводка отметок (State Map)

### 8.1. Назначение

Показать **распределение уже выбранных mood-тегов** за период. Не «где плохо/хорошо», а **где чаще ставили галочку**.

### 8.2. Визуал (Compose Canvas / custom layout)

**Слой 1 — три дуги-кольца (полярность)**

- Кольца concentric: внутреннее = POSITIVE, среднее = NEUTRAL, внешнее = NEGATIVE.
- Для группы G: `share(G) = eveningsWithAnyTagInG / totalEveningsInWindow`
- Отрисовка: дуга 0..360° (или сектор 120° на группу — design choice: **три сектора 120°** проще читать, чем три полных кольца).
- **Рекомендация:** три сектора одного круга (по 120°), радиус заливки сектора = `share * maxRadius` — «статистика долей», не уровни RPG.

**Слой 2 — 10-угольник**

- 10 осей по `MoodTag.orderedForUi`, равные углы.
- `score(T) = count(evenings with T) / totalEvenings`, clamp [0,1].
- Заливка полигона: `primary` ~25% alpha, обводка `primary` 60%.
- **Нет** пунктирного «идеального» шестиугольника.

**Слой 3 — легенда**

- Под картой: топ-3 тега по count + «всего вечеров с записями: N».
- Тап на имя тега в легенде → sheet дней (как у карточки).

### 8.3. Подпись-сводка (одна строка, алгоритм)

```
top2 = два тега с max count (score > 0)
if top2 empty → "Мало отметок за период"
else → "Чаще всего отмечали: {T1} и {T2}"
```

Без слов «дисбаланс», «проблема», «хорошо».

### 8.4. Состояния карты

| Записей | Карта | Подпись |
|---------|-------|---------|
| < 8 | Силуэт + пунктир | «Пока мало вечеров для сводки» |
| 8–11 | Полигон 40% alpha | «Предварительная сводка · N вечеров» |
| ≥ 12 | Полная | + строка top2 |

### 8.5. Тап → дни

- Тап сектора полярности → sheet: все вечера в окне, где был любой тег этой полярности.
- Тап вершины / пункт легенды → sheet: вечера с этим тегом.
- Sheet = тот же компонент, что у карточек (`InsightDaysSheet`).

---

## 9. Фаза 3 — 90 дней, связь карта ↔ карточки, баннер

### 9.1. Переключатель 30 / 90

- Segmented control в TopAppBar.
- Смена → пересчёт snapshot, сброс «Показать ещё» карточек.
- Copy: *«За последние {N} дней»*.

### 9.2. Подсветка связи

**При тапе на тег T на карте:**

1. `highlightedTag = T`
2. Карточки, где `pattern.involves(T)` — border `primary` 2dp; остальные 60% alpha.
3. Если карточек 0 — snackbar *«Отдельных закономерностей с «{T}» за период не нашлось»* (не ошибка).

**При тапе на карточку:**

1. `highlightedPatternId = id`
2. На карте подсветить вершины тегов паттерна; сектора полярности — optional glow.

**Сброс:** tap outside / повторный tap / смена 30/90.

### 9.3. Баннер на «Сегодня»

См. §3.3. Реализация: `JournalViewModel` + `UserPreferencesRepository` flags.

---

## 10. Фаза 4 — Gemma «Пояснить сводку» (опционально)

### 10.1. Условия показа

- `ModelSelectionRepository.attached != null`
- `AiEngineStatus.READY` (или после успешного warmUp)
- ≥ `MIN_ENTRIES_FULL` записей
- Блок **свёрнут** по умолчанию: «Спросить ментора о сводке»

### 10.2. Поведение

- По нажатию: `GemmaLocalEngine.explainInsights(snapshot, manifesto?)` — новый метод domain.
- Промпт: только факты — доли полярности, top теги, тексты **уже показанных** карточек (title+body), **без** полных дневников; max 3 цитаты ≤300 символов из записей паттернов.
- Стиль: `JournalPrompts` — 3–4 предложения, без списков, без диагноза, гипотезы с «возможно», «похоже».
- Timeout 120s, `Dispatchers.Default`, отмена при уходе с экрана.
- Без модели: блок скрыт (не mock).

### 10.3. Copy

- RU: *«Ментор прочитает только сводку отметок и закономерности на экране — не весь архив.»*

---

## 11. Доменная модель и архитектура

```
domain/insights/
  InsightPolicy.kt
  InsightSnapshot.kt          // period, windowDays, totalEvenings, polarityShares, tagScores[10], patterns
  InsightPattern.kt           // sealed types
  InsightPatternDetector.kt
  InsightPatternFormatter.kt
  StateMapCaptionBuilder.kt
  InsightsSnapshotBuilder.kt  // entries + language → InsightSnapshot

presentation/insights/
  InsightsContract.kt
  InsightsViewModel.kt
  InsightsScreen.kt
  components/
    StateMapView.kt
    InsightPatternCard.kt
    InsightDaysSheet.kt
    InsightsEmptyStates.kt
    InsightGemmaBlock.kt      // phase 4

domain/ai/
  GemmaLocalEngine.explainInsights(...)  // phase 4
```

**Зависимости VM:** `JournalRepository`, `AppLanguageResolver`, `Clock`; phase 4: `GemmaLocalEngine`, `ModelSelectionRepository`, `UserPreferencesRepository` (manifesto).

**Тесты:** `InsightPatternDetectorTest` (≥12 fixtures), `StateMapCaptionBuilderTest`, `InsightsSnapshotBuilderTest`.

**Room (optional cache, v1.1):**

```kotlin
@Entity(tableName = "insight_snapshots")
data class InsightSnapshotEntity(
  @PrimaryKey val windowDays: Int,
  val generatedAt: Long,
  val payloadJson: String,  // InsightSnapshot serialized
  val schemaVersion: Int = 1,
)
```

Инвалидация: при `save(entry)` удалять кэш для 30 и 90.

---

## 12. Копирайт — ключевые строки (RU / EN)

| Key | RU | EN |
|-----|----|----|
| `nav_insights` | Картина | Insights |
| `insights_subtitle` | Сводка отметок за период | Summary of your labels |
| `insights_not_a_score` | Это не оценка и не совет врача | Not a score or medical advice |
| `insights_period_30` | 30 дней | 30 days |
| `insights_period_90` | 90 дней | 90 days |
| `insights_section_map` | Сводка отметок | Label summary |
| `insights_section_patterns` | Закономерности | Patterns |
| `insights_empty_few_title` | Пока рано для сводки | Not enough evenings yet |
| `insights_empty_few_body` | Нужно хотя бы 12 вечеров с записями за {N} дней. Сейчас — {count}. | Need at least 12 evenings with entries in the last {N} days. You have {count}. |
| `insights_empty_patterns_title` | Явных повторов пока нет | No clear repeats yet |
| `insights_empty_patterns_body` | За период отметки менялись без устойчивых связок. | Your labels varied without strong links in this period. |
| `insights_show_more` | Показать ещё | Show more |
| `insights_days_sheet_title` | Вечера | Evenings |
| `insights_banner_title` | Достаточно вечеров для сводки | Enough evenings for a summary |
| `insights_banner_cta` | Открыть картину | Open Insights |
| `insights_mentor_cta` | Спросить ментора о сводке | Ask mentor about this summary |

---

## 13. Приватность и границы

- Всё on-device, SQLCipher как у журнала.
- В sheet и на карте **не** показывать полный текст рефлексии — только дата + теги (+ опционально первая строка microWins ≤80 символов, phase 4 only).
- Экспорт vault: snapshot не обязателен в v1.
- Не отправлять analytics на сервер (инвариант Sanctum).

---

## 14. План фаз и критерии приёмки

### Фаза 1 — MVP вкладки и карточек

**Deliverables:** Insights tab, types 1,2,3,5, sheets → detail, i18n, unit tests.

**Acceptance:**

- [ ] 5-й tab в bottom nav
- [ ] <12 записей → заглушка, нет crash
- [ ] ≥12 + паттерны → до 7 карточек, «Показать ещё»
- [ ] Tap card → days → EntryDetail
- [ ] Работает без Gemma и без сети
- [ ] Ни одна строка не содержит «оценка/дисбаланс/норма/нужно»

### Фаза 2 — Сводка отметок

**Deliverables:** StateMapView, legend, taps, caption, preview state 8–11.

**Acceptance:**

- [ ] Нет эталонного «идеального» полигона
- [ ] Тап тега → sheet дней
- [ ] Caption только «чаще отмечали», не «плохо/хорошо»

### Фаза 3 — Связность и рост

**Deliverables:** 30/90, highlight map↔cards, Today banner.

**Acceptance:**

- [ ] Смена периода пересчитывает карту и карточки
- [ ] Highlight сбрасывается предсказуемо
- [ ] Баннер не чаще 1/14 дней

### Фаза 4 — Gemma (optional)

**Deliverables:** `explainInsights`, UI block, coordinator routing.

**Acceptance:**

- [ ] Скрыт без модели
- [ ] Промпт не отправляет весь архив
- [ ] Отмена при уходе с экрана

---

## 15. Оценка трудозатрат (ориентир)

| Фаза | Календарь (1 dev) |
|------|-------------------|
| 1 | 1.5–2 недели |
| 2 | 1 неделя |
| 3 | 4–6 дней |
| 4 | 4–6 дней |
| **Итого** | **~4–5 недель** полный scope |

---

## 16. Чеклист решения «делаем / не делаем»

Перед планом разработки рекомендуется:

1. **Макет** статичной карты + 3 карточек (Figma или Compose preview) — показать 5 пользователям: *«Оценили или узнали себя?»*
2. **Порог 12** — согласовать с медианой реальных данных (если медиана &lt;8, снизить до 10).
3. **Имя вкладки** — «Картина» vs «Обзор» (A/B copy только в доке, не в коде).
4. **Фаза 4** — go/no-go отдельно от 1–3 (фазы 1–3 ценны без Gemma).

**Go**, если: ≥4/5 респондентов «узнал», 0/5 «меня оценили»; команда готова поддерживать `domain/insights` long-term.

**No-go / урезание**, если: респонденты читают карту как шкалу → оставить **только карточки** на вкладке без многоугольника, или отложить tab и показать 2 карточки в настройках как experiment.

---

## 17. Вне scope (явно)

- Obsidian-graph по записям, ручные `[[links]]`
- Embedding-модель, WorkManager batch, UMAP/t-SNE NDK
- Сравнение с другими пользователями
- Push «у вас дисбаланс»
- Тип 6 keyword cards (отдельный PR при необходимости)
- Замена недельного зеркала Gemma

---

*Конец документа. При принятии решения «go» — завести эпик `Insights` и разбить на PR: phase-1-tab-cards → phase-2-state-map → phase-3-linking → phase-4-mentor.*
