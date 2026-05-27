import React, { useState } from 'react';
import {
  Button,
  Callout,
  Card,
  CardBody,
  CardHeader,
  Divider,
  Grid,
  H1,
  H2,
  H3,
  Pill,
  Row,
  Spacer,
  Stack,
  Stat,
  Table,
  Text,
  useHostTheme,
} from 'cursor/canvas';

export default function UxUiAuditDashboard() {
  const theme = useHostTheme();
  const [activeTab, setActiveTab] = useState<'summary' | 'findings' | 'themes' | 'i18n'>('summary');
  const [expandedFinding, setExpandedFinding] = useState<string | null>(null);

  const toggleFinding = (id: string) => {
    setExpandedFinding(expandedFinding === id ? null : id);
  };

  // Inline CSS styles
  const styles = {
    container: {
      padding: '24px',
      background: theme.bg.editor,
      color: theme.text.primary,
      minHeight: '100%',
      fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    },
    header: {
      marginBottom: '24px',
    },
    tabs: {
      display: 'flex',
      gap: '8px',
      borderBottom: `1px solid ${theme.stroke.secondary}`,
      paddingBottom: '12px',
      marginBottom: '24px',
    },
    tabButton: (isActive: boolean) => ({
      background: isActive ? theme.accent.control : 'transparent',
      color: isActive ? theme.text.onAccent : theme.text.secondary,
      border: 'none',
      padding: '8px 16px',
      borderRadius: '8px',
      cursor: 'pointer',
      fontWeight: isActive ? (600 as const) : (400 as const),
      fontSize: '14px',
      transition: 'all 0.2s ease',
    }),
    cardItem: {
      padding: '16px',
      background: theme.bg.elevated,
      borderRadius: '12px',
      border: `1px solid ${theme.stroke.primary}`,
      marginBottom: '12px',
      cursor: 'pointer',
      transition: 'transform 0.15s ease, border-color 0.15s ease',
    },
    listHeader: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
    },
    expandedContent: {
      marginTop: '12px',
      paddingTop: '12px',
      borderTop: `1px solid ${theme.stroke.secondary}`,
      fontSize: '14px',
      lineHeight: '1.6',
    },
    tableHeader: {
      background: theme.fill.secondary,
      fontWeight: '600',
    },
    tableCell: {
      padding: '10px 14px',
      borderBottom: `1px solid ${theme.stroke.secondary}`,
    },
  };

  const strengths = [
    { title: "Empathic UX (Скрытие блоков)", desc: "Блок «Микро-победы» плавно скрывается (через AnimatedVisibility) при выборе негативных тегов (тревога, грусть). Это исключает токсичную оценку и чувство вины (guilt-tripping) в плохие дни." },
    { title: "Защита от двойных Insets", desc: "Обнуление contentWindowInsets во вложенных Scaffold (например, в JournalScreen) решает системную проблему Compose, исключая «мертвые зоны» и двойное наложение отступов." },
    { title: "Мягкая палитра (без красного негатива)", desc: "Теги с негативной полярностью (страх, раздражение) окрашиваются в мягкий лавандовый primaryContainer, а не в тревожный красный, что дарит чувство безопасности." },
    { title: "Прекрасный тон и копирайт", desc: "Тексты лишены клише. Формулировки вроде «Освободите голову — лист помнит за вас» или «Берегите себя» создают доверительный вечерний ритуал." },
    { title: "Умный Bootstrap & Warmup", desc: "Экран прогрева ИИ-модели использует пульсирующую «дыхательную» анимацию без раздражающих процентов. При сбое локального ИИ приложение не падает, а мягко переходит в offline-режим." }
  ];

  const findings = [
    {
      id: "f1",
      priority: "P1",
      priorityColor: "warning" as const,
      screen: "Итоги дня (Сегодня)",
      title: "Беззвучное обрезание Focus на завтра (3 строки)",
      observation: "Ввод пользователя жестко обрезается до 3 строк во ViewModel. При вводе или вставке 4-й строки символы просто игнорируются, клавиатура «каменеет» без предупреждения.",
      problem: "Пользователь считает это багом, зависанием приложения или дефектом ввода, что разрушает терапевтическое доверие.",
      recommendation: "Разрешить ввод больше 3 строк, но подсвечивать текстовое поле красной рамкой (error state) и выводить эмпатичную подсказку: «Задачи ограничены тремя строками, чтобы вы могли спокойно отдохнуть».",
      files: "JournalViewModel.kt (line 178), SoftTextField.kt"
    },
    {
      id: "f2",
      priority: "P1",
      priorityColor: "warning" as const,
      screen: "Чат / Сегодня",
      title: "Избыточная высота шапки экрана (Bloated Header)",
      observation: "В чате и на главном экране сверху друг под другом идут: TopAppBar, строка статуса ИИ-модели (AiEngineStatusChip), строка процентов контекста, и индикатор прогресса. Шапка занимает ~124dp.",
      problem: "Забирает драгоценное вертикальное пространство, особенно на небольших экранах и при открытой клавиатуре, зажимая сообщения чата в узкую щель.",
      recommendation: "Убрать отдельную строку для статуса ИИ. Разместить статус в виде маленькой цветной точки-индикатора (зеленый/желтый/серый) прямо в TopAppBar рядом с заголовком экрана.",
      files: "ChatScreen.kt (line 168), JournalScreen.kt (line 187)"
    },
    {
      id: "f3",
      priority: "P1",
      priorityColor: "warning" as const,
      screen: "Деталь записи (EntryDetailScreen)",
      title: "Пустая карточка SectionCard для тегов настроения",
      observation: "Для вывода тегов настроения рендерится пустой SectionCard { } с тегами, перечисленными в одну строку через точку в параметре subtitle карточки.",
      problem: "Выглядит как недоработка верстки или пустой серый прямоугольник. Текстовое перечисление блекнет на фоне красивых интерактивных чипов главного экрана.",
      recommendation: "Рендерить выбранные настроения внутри карточки в виде красивых, неактивных (read-only) чипов с их фирменными цветами полярности.",
      files: "EntryDetailScreen.kt (line 183)"
    },
    {
      id: "f2-del",
      priority: "P2",
      priorityColor: "neutral" as const,
      screen: "Деталь записи (EntryDetailScreen)",
      title: "Заградительное удаление одной записи",
      observation: "Для удаления одной записи дневника требуется двухэтапный диалог, где на втором этапе нужно вручную набрать слово «удалить» (или «delete» на EN).",
      problem: "Подобные барьеры оправданы в «Опасной зоне» при полном сбросе базы данных. Для удаления одной записи это создает избыточную рутину и раздражение.",
      recommendation: "Упростить удаление одной записи до стандартного диалога подтверждения с кнопкой «Удалить» (окрашенной в error/красный) без необходимости печатать слова.",
      files: "EntryDetailScreen.kt (line 227)"
    },
    {
      id: "f4",
      priority: "P2",
      priorityColor: "neutral" as const,
      screen: "Настройки (SettingsScreen)",
      title: "Слишком длинный плоский список (Wall of 9 Cards)",
      observation: "Экран содержит 9 отдельных карточек SectionCard, идущих подряд в одной колонке. Длина скролла огромна, пользователь теряется в обилии переключателей.",
      problem: "Ослабляет фокус, создает ощущение хаоса и сложности в приложении, позиционирующем себя как простое и «тихое».",
      recommendation: "Объединить 9 карточек в 4 логические группы под общими карточками с разделителями внутри: 1) Оформление и язык, 2) Безопасность и приватность, 3) ИИ-ментор и Напоминания, 4) Опасная зона.",
      files: "SettingsScreen.kt"
    },
    {
      id: "f5",
      priority: "P2",
      priorityColor: "neutral" as const,
      screen: "Чат (ChatScreen)",
      title: "Симметричные баблы сообщений",
      observation: "Все сообщения (пользователя и ИИ) используют абсолютно симметричное скругление RoundedCornerShape(16.dp).",
      problem: "Сложнее визуально сканировать диалог, баблы воспринимаются как статичные текстовые блоки, а не как живой диалог.",
      recommendation: "Использовать асимметричные скругления: для пользователя срезать правый нижний угол, для ИИ — левый нижний. Это золотой стандарт мессенджеров.",
      files: "ChatScreen.kt (line 291)"
    },
    {
      id: "f6",
      priority: "P2",
      priorityColor: "neutral" as const,
      screen: "Сегодня (JournalScreen)",
      title: "Утерянный приветственный девиз",
      observation: "Строка screen_subtitle («Тихо, без оценок. Только для вас») полностью переведена на оба языка, но исключена из интерфейса из-за экономии места в TopAppBar.",
      problem: "Новые пользователи не видят главного эмпатического обещания продукта при первом запуске.",
      recommendation: "Показывать этот девиз на пустом экране Истории («Здесь будет ваша история. Тихо, без оценок...») или в качестве временной приветственной карточки на главном экране при первом входе.",
      files: "strings.xml, JournalScreen.kt"
    },
    {
      id: "f7",
      priority: "P3",
      priorityColor: "neutral" as const,
      screen: "История (HistoryScreen)",
      title: "Информационная пустота ленты",
      observation: "Лента истории выглядит очень минималистично, но на карточках записей нет никакого намека на то, содержит ли день ответ ИИ-ментора или важные мысли.",
      problem: "Пользователю трудно ориентироваться и находить эмоциональные пики («теплые» дни, где он общался с ментором), приходится открывать всё подряд.",
      recommendation: "Отображать маленькую лавандовую иконку искры (Icons.Outlined.AutoAwesome) на карточке истории, если запись содержит отклик ИИ-ментора.",
      files: "HistoryScreen.kt (line 192)"
    }
  ];

  const quickWins = [
    { task: "Интеграция девиза на пустой экран", impact: "Высокий", effort: "< 2 часов", desc: "Добавить неиспользуемую строку screen_subtitle в EmptyState экрана истории, укрепив первое впечатление пользователя." },
    { task: "Асимметричные баблы в чате", impact: "Средний", effort: "1 час", desc: "Заменить RoundedCornerShape(16.dp) на асимметричные углы для баблов сообщений, мгновенно преобразив Conversational UI." },
    { task: "Чипы настроения в детали записи", impact: "Высокий", effort: "2-3 часа", desc: "Вместо пустого SectionCard рендерить выбранные настроения в виде аккуратных цветных чипов, вернув эстетику в EntryDetailScreen." },
    { task: "Замена spaces («  ») на padding", impact: "Низкий", effort: "1 час", desc: "Убрать хардкод пробелов в строках strings.xml для иконок в кнопках и использовать стандартные Compose-отступы." },
    { task: "Упрощение удаления одной записи", impact: "Высокий", effort: "1 час", desc: "Убрать необходимость печатать слово «удалить» при стирании одной записи, оставив безопасный одиночный диалог с красной кнопкой." }
  ];

  return (
    <div style={styles.container}>
      {/* Header */}
      <Stack style={styles.header} gap={8}>
        <Row align="center" justify="space-between">
          <H1>PocketReflect — UX/UI & Visual Audit</H1>
          <Pill tone="success">Май 2026</Pill>
        </Row>
        <Text tone="secondary">
          Локальный приватный дневник «итоги дня» на Android (Kotlin, Jetpack Compose, Material 3)
        </Text>
      </Stack>

      {/* Tabs */}
      <div style={styles.tabs}>
        <button
          style={styles.tabButton(activeTab === 'summary')}
          onClick={() => setActiveTab('summary')}
        >
          Резюме & Сильные стороны
        </button>
        <button
          style={styles.tabButton(activeTab === 'findings')}
          onClick={() => setActiveTab('findings')}
        >
          Находки ({findings.length})
        </button>
        <button
          style={styles.tabButton(activeTab === 'themes')}
          onClick={() => setActiveTab('themes')}
        >
          Тема: Светлая vs Тёмная
        </button>
        <button
          style={styles.tabButton(activeTab === 'i18n')}
          onClick={() => setActiveTab('i18n')}
        >
          i18n & Quick Wins
        </button>
      </div>

      {/* Tab 1: Summary */}
      {activeTab === 'summary' && (
        <Stack gap={20}>
          <Callout tone="success" title="Общая оценка: ОТЛИЧНО (9/10)">
            <Text>
              PocketReflect спроектирован с глубоким пониманием <strong>Empathic UX</strong>. В коде отсутствуют манипулятивные механики (стрики, геймификация, уведомления-принуждения). Продукт уважает пользователя, бережно оберегает его приватность на аппаратном уровне (отсутствие INTERNET в Манифесте, SQLCipher) и создает расслабляющий вечерний ритуал. Инженерная реализация Compose-инвариантов выполнена на высоком профессиональном уровне.
            </Text>
          </Callout>

          <H2>Топ-5 сильных сторон (Инварианты успеха)</H2>
          <Grid columns={1} gap={12}>
            {strengths.map((s, idx) => (
              <Card key={idx} variant="flat">
                <CardHeader>
                  <H3>{idx + 1}. {s.title}</H3>
                </CardHeader>
                <CardBody>
                  <Text tone="secondary">{s.desc}</Text>
                </CardBody>
              </Card>
            ))}
          </Grid>
        </Stack>
      )}

      {/* Tab 2: Findings */}
      {activeTab === 'findings' && (
        <Stack gap={16}>
          <H2>Карта находок (Кликните для деталей)</H2>
          <Text tone="secondary" size="small">
            Мы разделили находки по приоритетам: P1 (исправить до публикации), P2 (полировка опыта), P3 (задел на будущее). Критических дефектов P0 (ломающих работу) не обнаружено.
          </Text>

          <div>
            {findings.map((f) => {
              const isExpanded = expandedFinding === f.id;
              return (
                <div
                  key={f.id}
                  style={{
                    ...styles.cardItem,
                    borderColor: isExpanded ? theme.accent.primary : theme.stroke.primary,
                  }}
                  onClick={() => toggleFinding(f.id)}
                >
                  <div style={styles.listHeader}>
                    <Row gap={8} align="center">
                      <Pill tone={f.priorityColor}>{f.priority}</Pill>
                      <Text style={{ fontWeight: 600 }}>{f.title}</Text>
                    </Row>
                    <Row gap={8} align="center">
                      <Pill tone="neutral">{f.screen}</Pill>
                      <Text tone="tertiary" size="small">{isExpanded ? '▲' : '▼'}</Text>
                    </Row>
                  </div>

                  {isExpanded && (
                    <div style={styles.expandedContent}>
                      <Stack gap={12}>
                        <div>
                          <strong>Наблюдение:</strong>
                          <Text tone="secondary">{f.observation}</Text>
                        </div>
                        <div>
                          <strong>Почему это проблема:</strong>
                          <Text tone="secondary" style={{ color: theme.palette.orange[500] }}>
                            {f.problem}
                          </Text>
                        </div>
                        <div>
                          <strong>Рекомендация:</strong>
                          <Text tone="primary" style={{ fontWeight: 500 }}>
                            {f.recommendation}
                          </Text>
                        </div>
                        <Divider />
                        <Row justify="space-between" align="center">
                          <Text tone="tertiary" size="small">
                            Файлы: <code>{f.files}</code>
                          </Text>
                          <Pill tone="neutral">smoke-тест на устройстве</Pill>
                        </Row>
                      </Stack>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </Stack>
      )}

      {/* Tab 3: Themes */}
      {activeTab === 'themes' && (
        <Stack gap={20}>
          <H2>Оценка паритета Тем оформления</H2>
          <Text tone="secondary">
            Отказ от Material You (Dynamic Color) оправдан — палитра полностью подчинена терапевтическому UX. Светлая тема («Рассвет») выполнена так же профессионально, как и фирменная Тёмная («Полночь»), без перегрузки глаз.
          </Text>

          <Table
            headers={["Элемент / Аспект", "Тёмная тема (Midnight)", "Светлая тема (Dawn)", "Вердикт / Комментарий"]}
            rows={[
              ["Контраст текста", "15.2:1 (EAEAF4 на 0B1020). Идеальный контраст.", "14.4:1 (1A1F33 на F3F4FA). Идеальный контраст.", "OK — Исключительная читаемость на обоих экранах."],
              ["Усталость глаз", "Глубокий сине-черный цвет успокаивает нервную систему перед сном.", "Мягкий пастельный фон без бьющего белого цвета.", "OK — Паритет соблюден."],
              ["Подсветка тегов", "Негативные теги окрашены в лавандовый primaryContainer (комфорт).", "Используются те же мягкие лавандовые тона.", "OK — Эмпатичный дизайн работает везде."],
              ["Вспышка при запуске", "Splash-экран абсолютно темный (#0B1020). Идеальный cold start.", "Splash-экран остается темным. Вспышка при переходе в светлую.", "Замечание — Известный компромисс из-за ограничений ОС."],
              ["Текст Muted", "Contrast ~4:1 (8086A1 на 131A2E). Ниже стандарта AA (4.5:1).", "Contrast 4.7:1 (5C6478 на E8EBF5). Проходит AA.", "Полировка — В тёмной теме поднять яркость TextMuted."]
            ]}
          />

          <Callout tone="neutral" title="Техническая рекомендация по Cold Start Splash">
            <Text size="small">
              Чтобы победить темный splash в светлой теме без усложнений, можно использовать динамическую смену XML-темы в onCreate перед super.onCreate(), но так как это требует сохранения состояния темы в SharedPreferences (до инициализации DataStore), текущий компромисс является наиболее надежным.
            </Text>
          </Callout>
        </Stack>
      )}

      {/* Tab 4: i18n & Quick Wins */}
      {activeTab === 'i18n' && (
        <Stack gap={24}>
          <Stack gap={12}>
            <H2>Анализ локализации (i18n EN / RU)</H2>
            <Text tone="secondary">
              Перевод strings.xml выполнен очень качественно, но есть 2 небольших стилистических шероховатости в английском пакете.
            </Text>
            <Table
              headers={["Ключ", "Русская версия (strings.xml)", "Английская версия (strings-en.xml)", "Замечание / Лучшая альтернатива"]}
              rows={[
                ["tasks_title", "Фокус на завтра", "Tomorrow focus", "Немного сухо. Рекомендуется: 'Focus on Tomorrow' или 'Tomorrow's Focus'."],
                ["tomorrow_reminder_channel_description", "Мягкие напоминания о вчерашнем фокусе на завтра", "Gentle reminders about yesterday's tomorrow focus", "Смешно и путанно ('yesterday's tomorrow focus'). Рекомендуется: 'Gentle reminders of your plans for today'."],
                ["tomorrow_reminder_switch", "Напоминать утром о вчерашних задачах", "Remind in the morning about yesterday's tasks", "Выбивается из эмпатичного тона. Рекомендуется: 'Remind me in the morning about yesterday's focus'."]
              ]}
            />
          </Stack>

          <Divider />

          <Stack gap={12}>
            <H2>Quick Wins (Реализуемо за ≤ 1 день)</H2>
            <Text tone="secondary">
              Эти изменения практически бесплатны по сложности кода, но кардинально улучшают визуал и UX.
            </Text>
            <Grid columns={2} gap={12}>
              {quickWins.map((qw, idx) => (
                <Card key={idx} variant="flat">
                  <CardHeader trailing={<Pill tone="success">{qw.effort}</Pill>}>
                    <H3>{qw.task}</H3>
                  </CardHeader>
                  <CardBody>
                    <Text size="small" tone="secondary" style={{ marginBottom: '8px' }}>{qw.desc}</Text>
                    <Pill tone="neutral">Приоритет: {qw.impact}</Pill>
                  </CardBody>
                </Card>
              ))}
            </Grid>
          </Stack>

          <Divider />

          <Stack gap={12}>
            <H2>Чего делать НЕЛЬЗЯ (Абсолютные инварианты)</H2>
            <Callout tone="warning" title="Ограничения, которые нельзя нарушать">
              <Stack gap={8}>
                <Text size="small">• <strong>Никакого INTERNET разрешения:</strong> Не поддаваться искушению добавить облачную синхронизацию или аналитику Sentry. Приватность — главная ценность.</Text>
                <Text size="small">• <strong>Никаких оценок дня (стрики/звезды):</strong> Не вводить систему страйков («ты пропустил 3 дня!») или оценок. Это разрушает поддерживающий (empathic) тон.</Text>
                <Text size="small">• <strong>Никакого Dynamic Color:</strong> Не подключать Material You, чтобы цвета системы не сломали гармонию подобранной медитативной темы.</Text>
              </Stack>
            </Callout>
          </Stack>
        </Stack>
      )}
    </div>
  );
}
