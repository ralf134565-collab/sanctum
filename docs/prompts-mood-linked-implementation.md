# Implementation Specification: Mood-Linked Prompts — Sanctum

This technical specification outlines the architecture, algorithm, file changes, and testing plan required to implement the mood-linked prompt system in **Sanctum**.

---

## 1. Architectural Overview

Instead of picking prompts from a single flat pool, Sanctum will utilize a **Hybrid-Hierarchy Fallback Prompt Selection Engine**. This engine prioritizes tag-specific context, falls back to broader polarity pools, and uses a universal pool as a final safety net.

### 1.1 Taxonomy & Fallback Sequence
When a user selects a set of mood tags, the engine determines a single **dominant tag** based on clinical priority:
1.  **NEGATIVE** (`ANXIETY`, `SADNESS`, `IRRITATION`) — *Highest Priority.* Savoring prompts are blocked during distress.
2.  **NEUTRAL** (`TIRED`, `OVERWHELMED`) — *Medium Priority.* Focuses on fatigue validation and boundaries.
3.  **POSITIVE** (`CALM`, `JOY`, `GRATITUDE`, `FOCUSED`) — *Lowest Priority.* Focuses on active savoring and value alignment.

```
[User Selects Tags]
       │
       ▼
[Determine Dominant Tag] (Negative > Neutral > Positive)
       │
       ├─► Tag Pool available? (Filter History) ──► [Return Tag Prompt]
       │         │ (Exhausted)
       │         ▼
       ├─► Polarity Pool available? (Filter History) ──► [Return Polarity Prompt]
       │         │ (Exhausted)
       │         ▼
       └─► Universal Pool available? (Filter History) ──► [Return Universal Prompt]
                 │ (Exhausted)
                 ▼
           [Absolute Fallback] (Full Language Pool, ignore History)
```

---

## 2. API Design & Selection Algorithm

Create a robust selection contract in `DailyPrompts.kt`:

```kotlin
package com.pocketreflect.app.domain.prompts

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.model.MoodTag
import kotlin.random.Random

object DailyPrompts {

    /**
     * Context-aware selection of a daily prompt.
     * 
     * @param language Current system language.
     * @param selectedTags Set of mood tags selected by the user.
     * @param history Set of recently shown prompt strings (FIFO, typical size 7).
     * @param random Deterministic source of randomness for reproducibility in tests.
     */
    fun forContext(
        language: AppLanguage,
        selectedTags: Set<MoodTag>,
        history: Set<String>,
        random: Random = Random.Default
    ): String {
        val rules = MoodPromptPolicy.resolve(language, selectedTags)
        
        // Step 1: Attempt Tag-Specific Pool
        if (rules.dominantTag != null) {
            val tagPool = MoodPromptPolicy.getPoolForTag(language, rules.dominantTag)
            val availableFromTag = tagPool.filterNot { it in history }
            if (availableFromTag.isNotEmpty()) {
                return availableFromTag[random.nextInt(availableFromTag.size)]
            }
            
            // Step 2: Fall back to Polarity-Wide Pool (excluding history)
            val polarityPool = MoodPromptPolicy.getPoolForPolarity(language, rules.dominantTag.polarity)
            val availableFromPolarity = polarityPool.filterNot { it in history }
            if (availableFromPolarity.isNotEmpty()) {
                return availableFromPolarity[random.nextInt(availableFromPolarity.size)]
            }
        }
        
        // Step 3: Fall back to Universal Pool (excluding history)
        val universalPool = MoodPromptPolicy.getUniversalPool(language)
        val availableFromUniversal = universalPool.filterNot { it in history }
        if (availableFromUniversal.isNotEmpty()) {
            return availableFromUniversal[random.nextInt(availableFromUniversal.size)]
        }
        
        // Step 4: Absolute Fallback (Complete Language Pool, ignore history to avoid crashes)
        val fullPool = MoodPromptPolicy.getFullLanguagePool(language)
        return fullPool[random.nextInt(fullPool.size)]
    }
}
```

---

## 3. UI and State Lifecycle Integration

The prompt must update fluidly as the user interacts with the tags, but lock down once they start typing, ensuring zero disruption to their reflection flow.

### 3.1 Rules for Prompt Regeneration on Tag Toggle
1.  **If Reflection has begun:** If any text field (Reflection, Micro-Wins, Tomorrow Tasks, Custom Field) contains non-whitespace text, **DO NOT change the prompt** under any circumstances, even if tags are toggled.
2.  **If fields are blank:** When a tag is toggled and all input fields are blank, recalculate the prompt on-the-fly using `DailyPrompts.forContext`.
3.  **On Day Load:**
    *   If a journal entry already exists in the database for that day, load the saved `promptShown` as-is.
    *   If no entry exists, pick a context-aware prompt using `DailyPrompts.forContext` (initially with empty tags, pulling from the Universal Pool).

### 3.2 JournalViewModel Changes
Modify `JournalViewModel.kt` to make `handleToggleTag` coroutine-aware or fetch history smoothly:

```kotlin
private fun handleToggleTag(intent: JournalContract.Intent.ToggleTag) {
    _state.update { current ->
        val nextTags = current.selectedTags.toMutableSet().apply {
            if (!add(intent.tag)) remove(intent.tag)
        }
        val cleanedWins = if (nextTags.any { it.polarity == MoodTag.Polarity.NEGATIVE }) {
            ""
        } else {
            current.microWins
        }
        
        current.copy(
            selectedTags = nextTags,
            microWins = cleanedWins,
            aiResponse = null,
            isAiThinking = false,
        )
    }
    
    // Check if reflection is empty, and if so, trigger prompt recalculation
    val currentSnapshot = _state.value
    val isReflectionEmpty = currentSnapshot.reflection.isBlank() &&
            currentSnapshot.tomorrowTasks.isBlank() &&
            currentSnapshot.customFieldAnswer.isBlank() &&
            currentSnapshot.microWins.isBlank()
            
    if (isReflectionEmpty) {
        viewModelScope.launch {
            val history = promptsHistory.recent.first().toSet()
            val language = appLanguageResolver.resolvedNow()
            val nextPrompt = DailyPrompts.forContext(
                language = language,
                selectedTags = currentSnapshot.selectedTags,
                history = history
            )
            // Push to history ONLY if the prompt actually changes to avoid clogging history
            if (nextPrompt != currentSnapshot.dailyPrompt) {
                promptsHistory.push(nextPrompt)
                _state.update { it.copy(dailyPrompt = nextPrompt) }
            }
        }
    }
    aiJob?.cancel()
}
```

Make a matching adjustment in `handleReshufflePrompt` to utilize `selectedTags`:

```kotlin
private fun handleReshufflePrompt() {
    viewModelScope.launch {
        val current = _state.value.dailyPrompt
        val history = promptsHistory.recent.first().toSet() + current
        val language = appLanguageResolver.resolvedNow()
        val selectedTags = _state.value.selectedTags
        val next = DailyPrompts.forContext(
            language = language,
            selectedTags = selectedTags,
            history = history
        )
        promptsHistory.push(next)
        _state.update { it.copy(dailyPrompt = next) }
    }
}
```

---

## 4. Database Migration

No database migration is needed. The `promptShown` field in the `JournalEntry` entity remains a plain `String`.
*   **Existing Days:** When loading an existing day, `loadDay` reads `existing.promptShown` directly and sets it into the state.
*   **New Days:** New entries receive the mood-linked prompt, which is saved as a plain string upon executing `handleSaveDay()`.

---

## 5. Offline and Privacy Design
*   **Local Calculation:** All selection, taxonomy, and fallback logic occurs purely in memory.
*   **No Network Leak:** No analytics, logs, or external metadata are generated. Emotional tags remain on-device.

---

## 6. Testing Strategy & Test Plan

All invariants must be strictly checked using JUnit tests in `DailyPromptsTest.kt`.

### 6.1 Required Unit Tests:

1.  **Size Invariant:** Verify that each of the 9 mood tags contains $\ge 6$ unique prompts in both RU and EN, and the Universal Pool contains $\ge 12$ prompts in both.
2.  **No Exact Duplicates:** Verify that there are zero exact string duplicates inside the RU pool and EN pool.
3.  **Strict Selection Priority (Multi-tag Determinism):**
    *   Passing `selectedTags = {ANXIETY, JOY}` must resolve to `ANXIETY` (Negative).
    *   Passing `selectedTags = {TIRED, CALM}` must resolve to `TIRED` (Neutral).
    *   Passing `selectedTags = {JOY, CALM}` must pick either `JOY` or `CALM` (Positive).
4.  **No Invalidation:** Verify that calling `forContext` with `selectedTags = {ANXIETY}` never returns a savor-only prompt (e.g., from the Calm or Joy pools).
5.  **History Exclusivity:** Verify that recently shown prompts (contained in `history`) are never returned unless the candidate pools are entirely exhausted.
6.  **Full Fallback Safety:** Verify that if `history` contains all prompts, `forContext` gracefully returns a valid prompt from the entire pool without hanging or throwing exceptions.

---

## 7. Estimate of Work

| Step | Task | Target Files | Est. Hours |
| :--- | :--- | :--- | :--- |
| **1** | Research & Conceptualization | `docs/prompts-research-brief.md` | Completed |
| **2** | Prompt Audit & Taxonomy | `docs/prompts-audit.md` | Completed |
| **3** | Prompt Writing (66 RU + 66 EN) | `DailyPrompts.kt`, `DailyPromptsEn.kt` | 3 - 4 hrs |
| **4** | Engine Core (Taxonomy, Policy) | `MoodPromptPolicy.kt`, `DailyPrompts.kt` | 2 hrs |
| **5** | ViewModel & UI Hookup | `JournalViewModel.kt` | 1.5 hrs |
| **6** | Write Unit Tests | `DailyPromptsTest.kt` | 1.5 hrs |
| **Total**| | | **8 - 9 hours** |
