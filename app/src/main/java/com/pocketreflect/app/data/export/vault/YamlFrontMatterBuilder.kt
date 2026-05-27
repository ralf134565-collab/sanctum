// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

/**
 * Минимальный генератор YAML Front Matter без внешних библиотек.
 */
internal object YamlFrontMatterBuilder {

    fun build(fields: LinkedHashMap<String, YamlValue>): String {
        val body = buildString {
            fields.forEach { (key, value) ->
                append(key)
                append(':')
                append(' ')
                append(value.render())
                append('\n')
            }
        }
        return "---\n$body---\n"
    }

    sealed interface YamlValue {
        fun render(): String

        data class Plain(val value: String) : YamlValue {
            override fun render(): String = value
        }

        data class Quoted(val value: String) : YamlValue {
            override fun render(): String = "\"${escapeQuoted(value)}\""
        }

        data class Block(val value: String) : YamlValue {
            override fun render(): String {
                if (value.isBlank()) return "\"\""
                val lines = value.replace("\r\n", "\n").lines()
                return buildString {
                    append("|\n")
                    lines.forEach { line ->
                        append("  ")
                        append(line)
                        append('\n')
                    }
                }.trimEnd('\n')
            }
        }

        data class InlineList(val items: List<String>) : YamlValue {
            override fun render(): String =
                items.joinToString(prefix = "[", postfix = "]") { item ->
                    if (needsQuote(item)) "\"${escapeQuoted(item)}\"" else item
                }
        }

        data class BlockList(val items: List<String>) : YamlValue {
            override fun render(): String = buildString {
                append('\n')
                items.forEach { item ->
                    append("  - ")
                    append(if (needsQuote(item)) "\"${escapeQuoted(item)}\"" else item)
                    append('\n')
                }
            }.trimEnd('\n')
        }
    }

    private fun escapeQuoted(raw: String): String =
        raw.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun needsQuote(raw: String): Boolean =
        raw.any { it.isWhitespace() || it in SPECIAL } ||
            raw.contains(':') ||
            raw.contains('#')

    private val SPECIAL = setOf('[', ']', '{', '}', ',', '&', '*', '!', '|', '>', '\'', '@', '%')
}
