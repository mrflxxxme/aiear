# Формат экспорта в Obsidian (F3, should-destination — ADR-015)

> Канонический формат .md-заметки и механизм записи для `ObsidianAdapter`.
> Механизм ратифицирован фаундером (grill 2026-07-07 §2.4): **SAF-запись в папку vault
> (primary) + `obsidian://new` URI (fallback)**. Obsidian — should-destination:
> реализуется в F3, но его блок не держит гейт волны (ADR-015 §3).

## Имя файла

```
YYYY-MM-DD-HHmm-<slug>.md
```

- Дата/время — локальные, из `created_at` мысли.
- `<slug>` — транслит первых 3–5 слов `refined_text` (fallback: `raw_transcript`),
  нижний регистр, `-` как разделитель, ≤40 символов, безопасные для FS символы
  (`[a-z0-9-]`).
- Пример: `2026-07-08-1000-pozvonit-v-nalogovuyu.md`.
- Коллизия имени (та же минута, тот же slug, другой `thought_id`) → суффикс `-2`, `-3`…

## YAML-frontmatter

```yaml
---
title: "<заголовок — первая строка/суть refined_text>"
type: reminder            # type из thought.schema.json
tags: [налоговая, патент] # tags из Thought
created: 2026-07-08T10:00:00+03:00   # created_at, RFC3339
source: aiear             # константа — маркер происхождения
thought_id: 3f1c9e4a-...  # id мысли — ключ идемпотентности
---
```

## Тело заметки

Markdown:

1. `refined_text` (основной текст).
2. Если есть CTA — блок действия (напр. `> ⏰ Напоминание: 2026-07-08 10:00`).
3. Свёрнутый блок сырого транскрипта (опционально, для аудита):
   `%% raw: <raw_transcript> %%`.

## Механизм записи

| Приоритет | Механизм | Детали |
|---|---|---|
| Primary | **SAF (Storage Access Framework)** | Пользователь один раз выбирает папку vault (`ACTION_OPEN_DOCUMENT_TREE`, persistable URI permission); адаптер пишет .md-файл напрямую. Работает без установленного Obsidian в фоне. |
| Fallback | **`obsidian://new` URI** | Если SAF-доступ не выдан/отозван: `obsidian://new?vault=<vault>&name=<имя>&content=<url-encoded>`. Требует установленный Obsidian; открывает приложение (не фоново). |
| Degradation | Share/Markdown (F3 catch-all) | Если оба недоступны — мысль остаётся в inbox, пользователю подсказка настроить vault. |

## Идемпотентность

- Ключ — `thought_id` во frontmatter. Перед записью адаптер ищет в целевой папке файл
  с тем же `thought_id` (индекс экспортов в inbox-сторе — primary-механизм; скан
  frontmatter — восстановление).
- Повторный экспорт того же `thought_id` = перезапись существующего файла,
  не создание дубля. Retry из очереди F3 безопасен.
