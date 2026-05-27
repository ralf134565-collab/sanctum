// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.local.entity.AITrendProfile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

internal object MarkdownWeeklyProfileRenderer {

    fun render(profile: AITrendProfile, language: AppLanguage): String {
        val zone = ZoneId.systemDefault()
        val periodStart = Instant.ofEpochMilli(profile.periodStart).atZone(zone).toLocalDate()
        val periodEnd = Instant.ofEpochMilli(profile.periodEnd).atZone(zone).toLocalDate()
        val weekLabel = weekFileStem(profile.periodEnd, zone)

        val frontMatter = YamlFrontMatterBuilder.build(
            linkedMapOf(
                ObsidianExportSchema.FIELD_SCHEMA to
                    YamlFrontMatterBuilder.YamlValue.Plain(ObsidianExportSchema.SCHEMA_VERSION.toString()),
                "type" to YamlFrontMatterBuilder.YamlValue.Plain("weekly_profile"),
                "period_start" to YamlFrontMatterBuilder.YamlValue.Plain(periodStart.toString()),
                "period_end" to YamlFrontMatterBuilder.YamlValue.Plain(periodEnd.toString()),
                "generated_at" to YamlFrontMatterBuilder.YamlValue.Plain(profile.generatedAt.toString()),
                "entry_count" to YamlFrontMatterBuilder.YamlValue.Plain(profile.entryCount.toString()),
                ObsidianExportSchema.FIELD_TAGS to YamlFrontMatterBuilder.YamlValue.BlockList(
                    listOf("sanctum/weekly"),
                ),
            ),
        )

        val title = if (language.isEnglish) {
            "Weekly picture · $weekLabel"
        } else {
            "Недельная картина · $weekLabel"
        }

        return buildString {
            append(frontMatter)
            append('\n')
            append("# $title\n\n")
            append(profile.summary.trim())
            if (!profile.structuredJson.isNullOrBlank()) {
                append("\n\n```json\n")
                append(profile.structuredJson.trim())
                append("\n```\n")
            }
        }.trimEnd() + "\n"
    }

    fun weekFileName(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        "${weekFileStem(epochMillis, zone)}.md"

    private fun weekFileStem(epochMillis: Long, zone: ZoneId): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        val weekFields = WeekFields.ISO
        val week = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.get(weekFields.weekBasedYear())
        return "$year-W${week.toString().padStart(2, '0')}"
    }
}

internal object MarkdownManifestoRenderer {

    fun render(manifesto: String, language: AppLanguage): String {
        val frontMatter = YamlFrontMatterBuilder.build(
            linkedMapOf(
                ObsidianExportSchema.FIELD_SCHEMA to
                    YamlFrontMatterBuilder.YamlValue.Plain(ObsidianExportSchema.SCHEMA_VERSION.toString()),
                "type" to YamlFrontMatterBuilder.YamlValue.Plain("manifesto"),
                ObsidianExportSchema.FIELD_TAGS to YamlFrontMatterBuilder.YamlValue.BlockList(
                    listOf("sanctum/manifesto"),
                ),
            ),
        )
        val title = if (language.isEnglish) "Personal landmarks" else "Личные ориентиры"
        return buildString {
            append(frontMatter)
            append('\n')
            append("# $title\n\n")
            append(manifesto.trim().ifBlank { "—" })
            append('\n')
        }
    }
}

internal object VaultManifestBuilder {

    fun build(
        exportedAt: Long,
        entryCount: Int,
        weeklyCount: Int,
        includeManifesto: Boolean,
        localeTag: String,
        appVersion: String,
    ): String {
        val exportedDate = Instant.ofEpochMilli(exportedAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        return YamlFrontMatterBuilder.build(
            linkedMapOf(
                "sanctum_vault_schema" to YamlFrontMatterBuilder.YamlValue.Plain("1"),
                "sanctum_export_schema" to
                    YamlFrontMatterBuilder.YamlValue.Plain(ObsidianExportSchema.SCHEMA_VERSION.toString()),
                "exported_at" to YamlFrontMatterBuilder.YamlValue.Quoted(exportedDate),
                "entry_count" to YamlFrontMatterBuilder.YamlValue.Plain(entryCount.toString()),
                "weekly_profile_count" to YamlFrontMatterBuilder.YamlValue.Plain(weeklyCount.toString()),
                "includes_manifesto" to YamlFrontMatterBuilder.YamlValue.Plain(includeManifesto.toString()),
                "locale" to YamlFrontMatterBuilder.YamlValue.Quoted(localeTag),
                "app_version" to YamlFrontMatterBuilder.YamlValue.Quoted(appVersion),
            ),
        )
    }
}
