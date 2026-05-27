# Sanctum Markdown Vault Export — Schema v1

Obsidian / Logseq compatible export format.

## Archive layout

```
sanctum-export-YYYY-MM-DD/
  _meta/manifest.yaml
  journal/YYYY-MM-DD.md
  weekly/YYYY-Www.md          (optional)
  manifesto.md                (optional)
```

## Journal front matter (schema v1)

| Field | Type | Description |
|-------|------|-------------|
| `sanctum_schema` | int | Always `1` for this version |
| `date` | string | `YYYY-MM-DD` day bucket |
| `timestamp` | long | Epoch millis |
| `mood_tags` | string[] | Stable keys (`calm`, `focused`, …) |
| `mood_labels` | string[] | Human labels at export locale |
| `tomorrow_focus_lines` | int | Line count |
| `prompt` | string | Daily prompt shown |
| `custom_field_q` / `custom_field_a` | string | Personal field snapshot |
| `micro_wins` / `tomorrow_tasks` / `reflection` / `ai_reflection` | block | Body fields |
| `tags` | list | Obsidian tags (`sanctum/journal`, `mood/calm`) |

## Encrypted export

Extension `.sanctum-vault` — ZIP bytes encrypted with magic `PRFM`, same crypto as `.sanctum` device backup.

## Import

Sanctum imports `journal/*.md` files with `sanctum_schema: 1` only. Merge by `date` (day bucket).
