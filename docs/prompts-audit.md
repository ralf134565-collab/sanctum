# Audit of Current Prompt Corpus — Sanctum

This document reviews the initial 40 Russian and English prompts in `DailyPrompts.kt` and `DailyPromptsEn.kt`. It evaluates their alignment with Sanctum's tone, scientific validity, cognitive load, and psychological suitability.

---

## 1. Summary Metrics

*   **Total Audited:** 40 prompts
*   **Keep as is:** 33 prompts (82.5%) — highly aligned, can be mapped directly to specific mood pools or the universal fallback.
*   **Revise:** 6 prompts (15%) — require semantic adjustments, re-framing to remove subtle pressure, or strict mapping to positive-only pools to avoid emotional mismatch.
*   **Retire:** 1 prompt (2.5%) — violates Sanctum's "no productivity coaching" guideline; causes self-criticism during low-energy states.

---

## 2. Complete Audit Table

| # | RU Текст | Polarity | Подходящие теги | Научный Intent | Оценка | Вердикт & Комментарий |
| :-: | :--- | :---: | :--- | :--- | :-: | :--- |
| **1** | Где сегодня было тело — собрано, разбросано, тяжело, легко? | Universal | `CALM`, `TIRED`, `OVERWHELMED` | Somatic Labeling | 5/5 | **KEEP**. Excellent somatic grounding. Map to Calm, Tired, Overwhelmed, and Universal. |
| **2** | Какая часть тела сегодня просила внимания и не получила? | Neu / Neg | `TIRED`, `OVERWHELMED`, `ANXIETY`, `SADNESS` | Somatic Mindfulness | 5/5 | **KEEP**. Great mindfulness of unmet somatic needs. Map to Tired/Overwhelmed/Anxiety. |
| **3** | Если бы тело могло сказать одно слово о сегодня — какое? | Universal | `Universal` | Affective Labeling | 5/5 | **KEEP**. Elegant cognitive reduction to a single word. Map to Universal pool. |
| **4** | Где в теле сегодня жило напряжение, а где была опора? | Neu / Neg | `ANXIETY`, `OVERWHELMED`, `IRRITATION`, `TIRED` | Somatic Grounding | 5/5 | **KEEP**. Highlights somatic support during stress. Map to Anxiety, Overwhelmed, Irritation. |
| **5** | Сегодня вы дышали ровно или урывками? | Universal | `CALM`, `FOCUSED`, `ANXIETY` | Somatic Mindfulness | 3/5 | **REVISE**. A bit too binary (yes/no). Revise to be open-ended and restful. |
| **6** | Чей голос сегодня хотелось услышать? | Universal | `SADNESS`, `JOY`, `GRATITUDE` | Social Connection | 4/5 | **KEEP**. Emotional connection cue. Map to Sadness and Gratitude. |
| **7** | Кому сегодня хотелось бы сказать «спасибо» — даже мысленно? | Positive | `GRATITUDE`, `JOY` | Savoring (Gratitude) | 5/5 | **REVISE (Restrict)**. Keep text, but restrict strictly to `GRATITUDE`/`JOY` tags. Unsuitable for negative days. |
| **8** | Кому вы сегодня сказали «да», когда внутри было «нет»? | Neu / Neg | `OVERWHELMED`, `IRRITATION`, `TIRED` | ACT (Boundaries) | 5/5 | **KEEP**. Powerful boundary check. Map to Overwhelmed, Irritation, Tired pools. |
| **9** | С кем сегодня было легче всего быть собой? | Positive | `CALM`, `JOY`, `GRATITUDE` | Social Savoring | 5/5 | **KEEP (Restrict)**. Map to positive mood pools. |
| **10** | От кого вы сегодня ждали понимания — и заметили ли его? | Negative | `SADNESS`, `IRRITATION` | Emotional Acceptance | 3/5 | **REVISE**. Can trigger resentment if unfulfilled. Reframe gently to focus on unexpected kindness. |
| **11** | Какой момент дня вы захотите вспомнить через год? | Positive | `JOY`, `GRATITUDE` | Savoring | 5/5 | **KEEP (Restrict)**. Restrict to positive tags. Toxic if randomly shown during panic or severe sadness. |
| **12** | Что сегодня случилось такое, что станет видно только через неделю? | Universal | `FOCUSED`, `Universal` | Cognitive Widening | 4/5 | **KEEP**. Helps zoom out. Map to Focused and Universal fallback. |
| **13** | Что вы сегодня сделали для своего «через месяц»? | Positive | `FOCUSED` | Behavioral Activation | 3/5 | **RETIRE**. Violates "no productivity coaching". Induces guilt on exhausted days. Replace with values focus. |
| **14** | Если бы у этого дня было одно слово — какое? | Universal | `Universal` | Cognitive Offloading | 5/5 | **KEEP**. Perfect low-friction fallback prompt. |
| **15** | Какая часть дня прошла быстро, а какая растянулась? | Universal | `Universal` | Time Mindfulness | 4/5 | **KEEP**. Encourages observation of temporal pacing. |
| **16** | Какая мысль возвращалась чаще всего? Хотите ей что-то ответить? | Neg / Neu | `ANXIETY`, `IRRITATION` | ACT (Defusion) | 5/5 | **KEEP**. Great defusion exercise. Map to Anxiety, Irritation. |
| **17** | О чём вы сегодня думали и не сказали вслух? | Universal | `Universal` | Cognitive Offloading | 4/5 | **KEEP**. Safe container for hidden thoughts. Map to Irritation, Sadness, and Universal. |
| **18** | Какой вопрос вы сегодня себе задавали, не дожидаясь ответа? | Universal | `Universal` | Metacognitive Awareness | 4/5 | **KEEP**. Deep metacognitive reflection. Map to Universal fallback. |
| **19** | Что вы сегодня говорили себе строже, чем сказали бы близкому? | Negative | `SADNESS`, `ANXIETY`, `OVERWHELMED` | Self-Compassion | 5/5 | **KEEP**. Gentle self-criticism softening. Map to Sadness/Anxiety/Overwhelmed. |
| **20** | Какой внутренний голос звучал сегодня громче — поддерживающий или критикующий? | Neg / Neu | `SADNESS`, `ANXIETY` | Self-Compassion | 4/5 | **KEEP**. Increases awareness of internal critic. Map to Sadness/Anxiety. |
| **21** | Что сегодня было важнее, чем правильно? | Universal | `OVERWHELMED`, `CALM`, `IRRITATION` | ACT (Acceptance) | 5/5 | **KEEP**. Breaks perfectionism. Map to Overwhelmed, Calm, Irritation. |
| **22** | Где вы сегодня выбрали по-своему? | Positive | `FOCUSED`, `JOY` | ACT (Autonomy) | 5/5 | **KEEP**. High autonomy validation. Map to Focused, Joy. |
| **23** | Что бы вы сегодня сделали по-другому, если бы знали, что никто не оценит? | Universal | `Universal` | ACT (Autonomy) | 4/5 | **KEEP**. Wides perspective. Map to Universal fallback. |
| **24** | Чему сегодня хотелось бы научить себя более молодого? | Universal | `Universal` | Integration of Wisdom | 4/5 | **KEEP**. Distils life lessons. Map to Universal. |
| **25** | На что сегодня было не жаль времени? | Positive | `FOCUSED`, `CALM`, `GRATITUDE` | Savoring / Values | 5/5 | **KEEP**. Identifies authentic resource allocation. Map to positive tags. |
| **26** | Где сегодня был ваш предел — и как вы с ним обошлись? | Neu / Neg | `TIRED`, `OVERWHELMED` | Self-Compassion | 5/5 | **KEEP**. Respects psychological and somatic limits. Map to Tired/Overwhelmed. |
| **27** | От чего вы сегодня устали честнее, чем привыкли признавать? | Neutral | `TIRED`, `OVERWHELMED` | Emotional Honesty | 5/5 | **KEEP**. Excellent fatigue validation. Map to Tired/Overwhelmed. |
| **28** | Где сегодня была граница, которую вы заметили только когда её пересекли? | Neu / Neg | `OVERWHELMED`, `IRRITATION` | Boundary Awareness | 5/5 | **KEEP**. Helps identify boundary violations retrospectively. Map to Overwhelmed/Irritation. |
| **29** | Что сегодня далось дороже, чем выглядит со стороны? | Neu / Neg | `TIRED`, `OVERWHELMED`, `SADNESS` | Effort Validation | 5/5 | **KEEP**. Validates unseen mental effort. Map to Tired/Overwhelmed/Sadness. |
| **30** | Что вы сегодня держали из последних сил — и стоило ли оно того? | Negative | `OVERWHELMED`, `ANXIETY`, `IRRITATION` | ACT (Acceptance) | 3/5 | **REVISE**. "Стоило ли оно того" can spark self-blame or regret. Reframe to focus on gentle release. |
| **31** | Какое чувство сегодня не нашло слов? | Universal | `Universal` | Affective Labeling | 5/5 | **KEEP**. Foundational affective labeling tool. Map to Universal fallback. |
| **32** | Что сегодня было слишком — слишком громко, ярко, быстро, тихо? | Neu / Neg | `OVERWHELMED`, `ANXIETY`, `IRRITATION` | Somatic Mindfulness | 5/5 | **KEEP**. Somatic and sensory boundaries. Map to Overwhelmed/Anxiety/Irritation. |
| **33** | Какая эмоция сегодня прожила дольше всего? | Universal | `Universal` | Affective Labeling | 4/5 | **KEEP**. Tracks emotional persistence. Map to Universal fallback. |
| **34** | Что сегодня внутри хотелось бы рассказать только дневнику? | Universal | `Universal` | Safe Expression | 5/5 | **KEEP**. Deeply comforting and private. Map to Universal fallback. |
| **35** | Какое чувство сегодня вы пытались не заметить? | Neg / Neu | `ANXIETY`, `SADNESS`, `IRRITATION`, `TIRED` | Emotional Acceptance | 5/5 | **KEEP**. Soft exposure tool. Map to negative pools. |
| **36** | Что сегодня дало вам больше энергии, чем забрало? | Positive | `JOY`, `CALM`, `GRATITUDE` | Savoring | 4/5 | **REVISE (Restrict)**. Keep wording, but strictly map to Positive tags. Toxic if shown during exhaustion. |
| **37** | Какой крошечный момент сегодня заставил вас искренне улыбнуться? | Positive | `JOY`, `GRATITUDE` | Savoring | 5/5 | **REVISE (Restrict)**. Same as above; highly effective for positive states, invalidating for negative states. |
| **38** | Что хорошего вы сегодня заметили в окружающем мире, проходя мимо? | Positive | `CALM`, `JOY` | Mindfulness / Savoring | 4/5 | **KEEP**. Savoring simple external beauty. Map to Calm, Joy, Universal. |
| **39** | Кто из людей или животных сегодня сделал ваш день чуточку теплее? | Positive | `JOY`, `GRATITUDE` | Connection Savoring | 5/5 | **REVISE (Restrict)**. Strictly map to Joy, Gratitude. |
| **40** | Какая трудность сегодня оказалась не такой страшной, как вы думали? | Neg / Neu | `ANXIETY`, `OVERWHELMED` | CBT (Decatastrophizing) | 5/5 | **KEEP**. Promotes perspective-taking. Map to Anxiety, Overwhelmed. |

---

## 3. Vulnerabilities and Mismatches in the Current Pool

### 3.1 Cognitive Mismatch & Toxic Positivity
*   **The Problem:** In the flat pool implementation, if a user selected `ANXIETY` or `SADNESS`, they could randomly be served prompt #11 (*"Какой момент дня вы захотите вспомнить через год?"*) or #37 (*"Какой крошечный момент сегодня заставил вас искренне улыбнуться?"*).
*   **Psychological Impact:** Serving positive savoring prompts during peak anxiety, grief, or deep sadness triggers cognitive dissonance and frustration. It is perceived as a form of **digital gaslighting** or **toxic positivity**, leading the user to abandon the session because the app "doesn't get it."

### 3.2 Productivity pressure
*   **The Problem:** Prompt #13 (*"Что вы сегодня сделали для своего «через месяц»?"*) is framed as forward-looking productivity coaching.
*   **Psychological Impact:** For users marking `TIRED` or `OVERWHELMED`, this question imposes an immediate cognitive demand to prove their productivity. This directly contradicts Sanctum’s core philosophy: being a calm, pressure-free sanctuary.

### 3.3 Translation Issues (RU vs EN)
The English translation is mostly accurate but has a few minor tonal differences where the English version feels slightly more direct or transactional, whereas the Russian "вы" has a softer, more distant yet protective boundary.
*   *Prompt 10 RU:* "От кого вы сегодня ждали понимания — и заметили ли его?" is highly psychological but carries a risk of resentment.
*   *Prompt 30 RU:* "Что вы сегодня держали из последних сил — и стоило ли оно того?" -> *"What did you hold up with your last strength today — and was it worth it?"* carries a subtle "blame" trigger in both languages ("was it worth it?" implies a poor investment of energy). We need to reframe this to focus on the somatic act of release.

---

## 4. Revision Proposals for Current Prompts

### Prompt #5
*   **Old RU:** "Сегодня вы дышали ровно или урывками?"
*   **Old EN:** "Did you breathe steadily today, or in short bursts?"
*   **New RU:** "Каким было ваше дыхание сегодня, когда день шёл на спад — ровным или торопливым?"
*   **New EN:** "How did your breath feel as the day began to slow down — steady or hurried?"
*   *Why:* Converts a binary yes/no question into a descriptive, soothing somatic exploration.

### Prompt #10
*   **Old RU:** "От кого вы сегодня ждали понимания — и заметили ли его?"
*   **Old EN:** "From whom were you waiting for understanding today — and did you notice it?"
*   **New RU:** "Какое тёплое или бережное слово сегодня согрело вас, даже если оно прозвучало тихо?"
*   **New EN:** "What warm or gentle word comforted you today, even if it was spoken softly?"
*   *Why:* Shifts focus away from frustrated expectations of other people and onto the somatic absorption of gentle support.

### Prompt #30
*   **Old RU:** "Что вы сегодня держали из последних сил — и стоило ли оно того?"
*   **Old EN:** "What did you hold up with your last strength today — and was it worth it?"
*   **New RU:** "Что вы сегодня несли из последних сил — и как ощущается возможность отпустить это теперь?"
*   **New EN:** "What did you carry with your last bit of strength today — and how does it feel to lay it down now?"
*   *Why:* Swaps judgment ("was it worth it?") for relief ("how does it feel to lay it down now?"), embodying ACT acceptance.
