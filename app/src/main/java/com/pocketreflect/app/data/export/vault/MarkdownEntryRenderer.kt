// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag

internal object MarkdownEntryRenderer {

    fun render(entry: JournalEntry, language: AppLanguage): String {
        val moodTags = entry.moodTags.map { it.storageKey }
        val moodLabels = entry.moodTags.map { tag -> tag.displayName(language) }
        val obsidianTags = buildList {
            add("sanctum/journal")
            moodTags.forEach { add("mood/$it") }
        }
        val tomorrowLines = if (entry.tomorrowTasks.isBlank()) {
            0
        } else {
            entry.tomorrowTasks.lineSequence().count()
        }

        val frontMatter = YamlFrontMatterBuilder.build(
            linkedMapOf(
                ObsidianExportSchema.FIELD_SCHEMA to
                    YamlFrontMatterBuilder.YamlValue.Plain(ObsidianExportSchema.SCHEMA_VERSION.toString()),
                ObsidianExportSchema.FIELD_DATE to
                    YamlFrontMatterBuilder.YamlValue.Plain(entry.dayBucket),
                ObsidianExportSchema.FIELD_TIMESTAMP to
                    YamlFrontMatterBuilder.YamlValue.Plain(entry.timestamp.toString()),
                ObsidianExportSchema.FIELD_MOOD_TAGS to
                    YamlFrontMatterBuilder.YamlValue.InlineList(moodTags),
                ObsidianExportSchema.FIELD_MOOD_LABELS to
                    YamlFrontMatterBuilder.YamlValue.InlineList(moodLabels),
                ObsidianExportSchema.FIELD_TOMORROW_LINES to
                    YamlFrontMatterBuilder.YamlValue.Plain(tomorrowLines.toString()),
                ObsidianExportSchema.FIELD_PROMPT to
                    YamlFrontMatterBuilder.YamlValue.Quoted(entry.promptShown),
                ObsidianExportSchema.FIELD_CUSTOM_Q to
                    YamlFrontMatterBuilder.YamlValue.Quoted(entry.customFieldQuestion),
                ObsidianExportSchema.FIELD_CUSTOM_A to
                    YamlFrontMatterBuilder.YamlValue.Quoted(entry.customFieldAnswer),
                ObsidianExportSchema.FIELD_MICRO_WINS to
                    YamlFrontMatterBuilder.YamlValue.Block(entry.microWins),
                ObsidianExportSchema.FIELD_TOMORROW to
                    YamlFrontMatterBuilder.YamlValue.Block(entry.tomorrowTasks),
                ObsidianExportSchema.FIELD_REFLECTION to
                    YamlFrontMatterBuilder.YamlValue.Block(entry.reflection),
                ObsidianExportSchema.FIELD_AI_REFLECTION to
                    YamlFrontMatterBuilder.YamlValue.Block(entry.aiReflection.orEmpty()),
                ObsidianExportSchema.FIELD_TAGS to
                    YamlFrontMatterBuilder.YamlValue.BlockList(obsidianTags),
            ),
        )

        val title = if (language.isEnglish) {
            "End of day · ${entry.dayBucket}"
        } else {
            "Итоги дня · ${entry.dayBucket}"
        }

        val moodHeader = if (language.isEnglish) "Mood" else "Настроение"
        val tomorrowHeader = if (language.isEnglish) "Tomorrow focus" else "Фокус на завтра"
        val winsHeader = if (language.isEnglish) "Micro-wins" else "Микро-победы"
        val reflectionHeader = if (language.isEnglish) "Reflection" else "Рефлексия"
        val promptHeader = if (language.isEnglish) "Daily prompt" else "Промпт дня"
        val mentorHeader = if (language.isEnglish) "Mentor response" else "Отклик ментора"
        val customHeader = if (language.isEnglish) "Personal field" else "Личное поле"

        return buildString {
            append(frontMatter)
            append('\n')
            append("# $title\n\n")

            append("## $moodHeader\n")
            append(moodLabels.joinToString(", ").ifBlank { "—" })
            append("\n\n")

            if (entry.tomorrowTasks.isNotBlank()) {
                append("## $tomorrowHeader\n")
                append(entry.tomorrowTasks.trim())
                append("\n\n")
            }

            if (entry.microWins.isNotBlank()) {
                append("## $winsHeader\n")
                append(entry.microWins.trim())
                append("\n\n")
            }

            if (entry.promptShown.isNotBlank()) {
                append("## $promptHeader\n")
                append(entry.promptShown.trim())
                append("\n\n")
            }

            if (entry.reflection.isNotBlank()) {
                append("## $reflectionHeader\n")
                append(entry.reflection.trim())
                append("\n\n")
            }

            if (entry.customFieldQuestion.isNotBlank() || entry.customFieldAnswer.isNotBlank()) {
                append("## $customHeader\n")
                if (entry.customFieldQuestion.isNotBlank()) {
                    append("**")
                    append(entry.customFieldQuestion.trim())
                    append("**\n\n")
                }
                if (entry.customFieldAnswer.isNotBlank()) {
                    append(entry.customFieldAnswer.trim())
                    append("\n\n")
                }
            }

            if (!entry.aiReflection.isNullOrBlank()) {
                append("## $mentorHeader\n")
                append(entry.aiReflection.trim())
                append('\n')
            }
        }.trimEnd() + "\n"
    }
}
