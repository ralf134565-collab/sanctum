# План: единая политика доступа к БД и исправление long-background бага

**Статус:** реализовано  
**Дата:** 2026-05-31  
**Контекст:** после долгого сворачивания (процесс жив) при **выключенной** биометрии — ошибка журнала, бесконечная загрузка истории, краш чата. Корень: БД закрывается в фоне, auto-lock сбрасывает сессию, но UI-разблокировки нет.

**Выбранная продуктовая модель:** вариант «простой» — runtime lock/unlock цикл **только** при включённой биометрии.

---

## Согласование с обещаниями приложения

### Не противоречит

| Источник | Обещание | После fix |
|----------|----------|-----------|
| `SECURITY.md` Threat Model | «Casual snooping» закрывается **biometric lock screen** | Без изменений: lock screen работает при `biometricLockEnabled = true` |
| `SECURITY.md` | Runtime lock + wipe passphrase from RAM при уходе в фон — часть **biometric gate** | Сохраняется **только** когда биометрия включена |
| `strings.xml` / `security_lock_subtitle` | Защита «только вы откроете дневник» — про переключатель биометрии | Точное соответствие: без lock — нет app-level gate |
| `privacy_details_body` | AES-256 SQLCipher, данные на устройстве, нет INTERNET | Без изменений — шифрование **на диске** всегда |
| `PRIVACY` §4 Biometric API | Биометрия **для разблокировки приложения** (опционально) | Опциональность подтверждается поведением |
| `manual-qa-checklist` §1.1.5 | С замком выкл. — cold start без BiometricGate | Дополняем: long background тоже без поломок |
| UI настроек | Auto-lock виден **только** при включённом lock | Backend будет соответствовать UI |

### Требует уточнения в документации (не ломает обещание, но сейчас формулировка шире реализации)

| Источник | Текущий текст | Проблема | Действие |
|----------|---------------|----------|----------|
| `PRIVACY.ru.md` §2 «Стирание памяти» | Passphrase зануляется в RAM «сразу после расшифровки» | Можно прочитать как «всегда при любом режиме» | Уточнить: при **включённой** биометрии — также при уходе в фон (runtime lock); при выключенной — защита = шифрование на диске + sandbox Android |
| `PRIVACY.md` §2 «Zero-Memory Footprint» | Аналогично | То же | Синхронное уточнение EN |

**Вывод:** вариант «простой» **не противоречит** threat model и UI. Единственное — смягчение RAM-wipe без биометрии, что согласуется с `SECURITY.md` (runtime lock = biometric gate) и осознанным отказом пользователя от lock в настройках.

---

## Целевая архитектура

```
                    biometricLockEnabled?
                           │
              ┌────────────┴────────────┐
              │ false                   │ true
              ▼                         ▼
     isAuthenticated = always true   onStop → lockDatabase()
     onStop → no-op                  onStart/onResume → tryUnlock / requiresAuth
     DB stays open while process     timeout → BiometricGate → markAuthenticated()
              │                         │
              └────────────┬────────────┘
                           ▼
              DatabaseAccess (whenReady / observeWhenReady)
                           │
              JournalRepository / RoomChatRepository
                           │
              ViewModels (единый error handling)
```

---

## Фазы реализации

### Фаза A — Политика блокировки (корень бага)

**Цель:** lifecycle lock/unlock/auto-lock только при `biometricLockEnabled = true`.

#### A.1 `AuthSessionHolder`

Файл: `app/src/main/java/com/pocketreflect/app/core/security/AuthSessionHolder.kt`

- Добавить явную политику (метод или отдельный `DatabaseLockPolicy` @Singleton):
  - `fun shouldUseRuntimeLock(biometricLockEnabled: Boolean): Boolean = biometricLockEnabled`
- `onAppBackgrounded()`: lock **только** если runtime lock активен.
- `tryUnlockIfSessionValid()`: no-op если runtime lock выключен; иначе текущая логика.
- `requiresAuth()`: если runtime lock выключен → `unlockDatabase()`, вернуть `false` (не требует auth).
- При переключении lock **off → on** (из Settings): `lockDatabase()` + `isAuthenticated = false` — пользователь должен пройти биометрию.
- При переключении lock **on → off**: `markAuthenticated()` — немедленный доступ.

#### A.2 `ProcessLifecycleAuthObserver`

Файл: `app/src/main/java/com/pocketreflect/app/core/security/ProcessLifecycleAuthObserver.kt`

- В `onStart` / опосредованно в `onStop`: читать `biometricLockEnabled` (кэш или `first()` как сейчас для timeout).
- Передавать флаг в методы `AuthSessionHolder`.

#### A.3 `BiometricGateViewModel`

Файл: `app/src/main/java/com/pocketreflect/app/core/security/BiometricGateViewModel.kt`

- `onAppResumed()`: вызывать `requiresAuth` только если lock enabled (или holder сам no-op — достаточно одного места).

#### A.4 `PocketReflectApp.onCreate`

Файл: `app/src/main/java/com/pocketreflect/app/PocketReflectApp.kt`

- Без изменений логики: lock off → `markAuthenticated()`; lock on → `lockDatabase()`.

#### A.5 Реакция на смену настройки lock в Settings

Файл: `app/src/main/java/com/pocketreflect/app/presentation/settings/SettingsViewModel.kt`

- При `SetBiometricLockEnabled(true)`: `authSessionHolder.lockDatabase()` + сброс сессии.
- При `SetBiometricLockEnabled(false)`: `authSessionHolder.markAuthenticated()`.

**Тесты:**
- `AuthSessionHolderTest` (новый) или расширить `RoomDatabaseProviderTest`:
  - lock off → background → advance time > timeout → foreground → DB open, authenticated.
  - lock on → background → timeout → requiresAuth → not authenticated, DB locked.
  - lock on → background → within timeout → DB open.

---

### Фаза B — Единый слой доступа к БД

**Цель:** один контракт для Flow и suspend; убрать `emptyFlow()`-ловушку.

#### B.1 Новый `DatabaseAccess`

Файл: `app/src/main/java/com/pocketreflect/app/core/security/DatabaseAccess.kt` (новый)

```kotlin
interface DatabaseAccess {
    val isReady: Flow<Boolean>  // authenticated && !locked

    suspend fun <T> whenReady(block: suspend () -> T): T

    fun <T> observeWhenReady(block: () -> Flow<T>): Flow<T>
}
```

Реализация `DefaultDatabaseAccess`:
- `isReady` = текущий `dbReadyFlow()` (combine auth + isLocked + revision).
- `whenReady`: `isReady.first { it }` затем block; при отмене — проброс.
- `observeWhenReady`:
  ```kotlin
  isReady.flatMapLatest { ready ->
      if (ready) block().catch { ... }
      else flowOf(null as T?) // или NEVER + distinct — см. B.2
  }
  ```
- DI: `@Singleton` в `SecurityModule` / новый модуль.

#### B.2 Замена `emptyFlow()`

**Было:** `emptyFlow()` → `combine()` не эмитит → вечный loading.

**Станет:** `observeWhenReady` эмитит:
- пока не ready — **не эмитить данные**, но использовать `flatMapLatest` на `isReady` так, чтобы при переходе `false → true` подписка пересоздавалась;
- альтернатива: wrapper `Flow<DatabaseLoadState<T>>` (Loading / Ready(T) / Failed).

**Рекомендация:** `isReady.flatMapLatest { if (it) dao.observe() else emptyFlow() }` **оставляет баг**. Правильно:

```kotlin
isReady.flatMapLatest { ready ->
    if (!ready) flowOf(null) // sentinel OR use transformWhile
    else block()
}
```

Для `combine` в History: если messages = null → State(isLoading=true), если empty list → empty state.

Или проще: **`whenReady` для первой эмиссии** + Room observe только когда ready — `flatMapLatest` на `isReady.filter { it }`.

#### B.3 Рефакторинг репозиториев

Файлы:
- `app/src/main/java/com/pocketreflect/app/data/repository/JournalRepository.kt`
- `app/src/main/java/com/pocketreflect/app/data/repository/RoomChatRepository.kt`

- Удалить локальные `dbReadyFlow()`.
- Flow-методы → `databaseAccess.observeWhenReady { journalDao().observeAll() }`.
- Suspend-методы → `databaseAccess.whenReady { journalDao().findByDay(...) }`.
- `journalDao()` / `chatMessageDao()` — только внутри `whenReady` / `observeWhenReady`.

**Затронутые suspend-потребители (получат fix автоматически):**
- JournalViewModel (findByDay, save, trends, …)
- ChatViewModel (insert, clear, …)
- EntryDetailViewModel (delete)
- BackupRepository, DefaultVaultExportRepository
- UserDataRepository (wipe)

---

### Фаза C — UI gate и обработка ошибок

#### C.1 Расширение `DatabaseGate` (страховка)

Файл: `app/src/main/java/com/pocketreflect/app/presentation/bootstrap/DatabaseGate.kt`

Сейчас: только `DatabaseAccessStatus.Blocked` (битая БД / миграция).

Добавить (опционально, low priority если A+B надёжны):
- При `biometricLockEnabled && databaseProvider.isLocked` — показывать тот же blank background что BiometricGate Loading, **не** RootScaffold.

**Решение:** можно **не делать**, если BiometricGate уже блокирует content при lock on. При lock off DB never locked → gate не нужен.

#### C.2 `ChatViewModel` — error handling

Файл: `app/src/main/java/com/pocketreflect/app/presentation/chat/ChatViewModel.kt`

- Обернуть `sendMessage()`, `clearChat()` в `try/catch (Throwable)`.
- При ошибке: snackbar (`R.string.chat_send_error` — добавить строку EN/RU), восстановить `inputText`, сбросить `isStreaming`.
- `compactChat()` — уже имеет catch; убедиться что сообщение понятное.

#### C.3 Единый паттерн ошибок (минимальный)

Journal — уже OK. EntryDetail delete — OK. Chat — fix в C.2.

Не вводить общий base class — только выровнять Chat по образцу Journal.

---

### Фаза D — Reactive reload после unlock

**Цель:** при lock **on**, если VM пережил unlock (edge case), данные перезагрузятся.

#### D.1 `JournalViewModel`

- Подписка на `databaseAccess.isReady` или `databaseProvider.revision`:
  - при переходе `false → true` и `bootstrapFailed || selectedDayBucket set` → `loadDay(...)`.

#### D.2 `HistoryViewModel`

- Подписка на `isReady`: при `false → true` → `reloadSignal++`.

#### D.3 `ChatViewModel`

- При `false → true` — state подтянется через observe pipeline; явный reload не обязателен если B.2 корректен.

#### D.4 `EntryDetailViewModel`

- Аналогично History — `observeById` после B.2 сам переподпишется через `flatMapLatest`.

**Приоритет D:** средний (после A+B; для lock-on сценария и тестов).

---

### Фаза E — Тесты

#### E.1 Unit

| Тест | Файл |
|------|------|
| AuthSessionHolder: lock off, long background | `AuthSessionHolderTest.kt` (новый) |
| AuthSessionHolder: lock on, timeout, unlock | тот же |
| DatabaseAccess.whenReady waits then executes | `DatabaseAccessTest.kt` (новый) |
| observeWhenReady emits after ready | тот же |
| RoomDatabaseProvider unlock/lock | существующий, расширить |

#### E.2 ViewModel

| Тест | Файл |
|------|------|
| ChatViewModel sendMessage DB failure → no crash, snackbar | `ChatViewModelTest.kt` |
| HistoryViewModel: ready delayed → eventually loads | `HistoryViewModelTest.kt` (новый или расширить) |

#### E.3 Integration / Robolectric

- Process lifecycle simulation: `ProcessLifecycleAuthObserver` + fake clock + biometric off/on.

---

### Фаза F — Документация и QA

#### F.1 Privacy policy

Файлы: `PRIVACY.md`, `PRIVACY.ru.md`

§2 — уточнить RAM wiping:

> **RU:** Ключ расшифровки в RAM стирается при закрытии сеанса работы с базой. Если включена биометрическая блокировка, база закрывается и ключ стирается при каждом уходе приложения в фон до повторной разблокировки. Если блокировка выключена, данные остаются зашифрованными на диске (AES-256); защита от чтения другими приложениями обеспечивается песочницей Android.

> **EN:** аналогично.

#### F.2 Manual QA checklist

Файл: `docs/manual-qa-checklist.md`

Добавить в §7.1 Жизненный цикл:

| # | Шаги | Ожидание |
|---|------|----------|
| 7.1.4 | Биометрия **выкл.** → свернуть > 2 мин → открыть → сменить день / история / отправить в чат | Всё работает, без ошибок и крашей |
| 7.1.5 | Биометрия **вкл.** → свернуть > таймаута → разблокировать | Данные загружаются после биометрии |

#### F.3 SECURITY.md

Опционально одна фраза в «Out of Scope»: без biometric lock app-level runtime lock не применяется; защита = encryption at rest.

---

## Порядок PR / коммитов

```
1. [A] AuthSessionHolder + Observer + Settings toggle + tests
2. [B] DatabaseAccess + repository refactor + tests
3. [C] ChatViewModel error handling + strings
4. [D] ViewModel reload hooks (optional same PR as B)
5. [F] Docs + QA checklist
```

Рекомендуется **2 PR**:
- **PR1 (critical):** A + B + C + E + F
- **PR2 (hardening):** D + доп. integration tests

Или один PR, если объём приемлем (~15–20 файлов).

---

## Файлы (полный список)

| Действие | Файл |
|----------|------|
| Изменить | `AuthSessionHolder.kt` |
| Изменить | `ProcessLifecycleAuthObserver.kt` |
| Изменить | `BiometricGateViewModel.kt` |
| Изменить | `SettingsViewModel.kt` |
| **Создать** | `DatabaseAccess.kt` |
| Изменить | DI module (`SecurityModule.kt` или `RepositoryModule.kt`) |
| Изменить | `JournalRepository.kt` |
| Изменить | `RoomChatRepository.kt` |
| Изменить | `ChatViewModel.kt` |
| Изменить | `JournalViewModel.kt` (D) |
| Изменить | `HistoryViewModel.kt` (D) |
| Изменить | `values/strings.xml`, `values-ru/strings.xml` |
| **Создать** | `AuthSessionHolderTest.kt` |
| **Создать** | `DatabaseAccessTest.kt` |
| Изменить | `ChatViewModelTest.kt` |
| Изменить | `PRIVACY.md`, `PRIVACY.ru.md` |
| Изменить | `docs/manual-qa-checklist.md` |

---

## Риски и mitigations

| Риск | Mitigation |
|------|------------|
| Пользователь включил lock, ожидает RAM wipe — получает | Поведение не меняется при lock on |
| Toggle lock off→on без немедленного prompt | Settings сразу lockDatabase + isAuthenticated=false; при следующем resume — gate |
| Регрессия backup/export в фоне | Suspend через whenReady; export только из UI когда app foreground |
| `whenReady` deadlock если lock on и user не auth | `isReady` остаётся false — UI на BiometricGate, не на RootScaffold |
| Passphrase в RAM дольше без lock | Документировано; соответствует выбору пользователя |

---

## Критерии готовности (Definition of Done)

- [ ] Биометрия **выкл.**: background > 5 min → journal / history / chat / save работают
- [ ] Биометрия **вкл.**: background > timeout → lock screen → unlock → всё работает
- [ ] Auto-lock не применяется при выключенной биометрии (unit test)
- [ ] Нет `emptyFlow()` в db-gated observe paths
- [ ] Chat send не крашит процесс при DB error
- [ ] Все unit-тесты зелёные
- [ ] PRIVACY.md / PRIVACY.ru.md синхронизированы
- [ ] manual-qa-checklist дополнен

---

## Вне scope (осознанно)

- Привязка KeyStore к биометрии (SECURITY.md — intentional trade-off)
- Lock БД в фоне без биометрии «для параноиков»
- Foreground service / WorkManager access к БД в locked state
