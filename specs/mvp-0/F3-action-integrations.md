---
phase: MVP0-F3
title: Фиксация в действие (интеграции-ядро)
wave: mvp-0
status: approved
tier: 4
pipeline: fullstack
deps: [MVP0-F2]
adr_refs: [ADR-004, ADR-007, ADR-015]
ui_spec: true
prd_refs: ["§8 MVP-0 F3", "§5"]
---

# MVP0-F3 — Фиксация в действие

## User story
Как пользователь, я хочу, чтобы структурированная мысль **сама легла** в мою систему (Obsidian / календарь / Telegram), потому что пойманная мысль без действия = потерянная мысль.

## Scope (destinations-ядро, ADR-015)
**Must-набор (держит гейт волны):**
- **Notes (inbox)** — встроенные заметки приложения; каждая мысль уже там (ADR-015), destination «inbox» = «остаётся во встроенных заметках».
- **Share Sheet / Markdown-экспорт** (catch-all).
- **Системный Календарь / Напоминания** (Android Calendar/Intents).
- **Telegram** (отправка себе).

**Should (едет в F3, но его блок НЕ держит гейт волны — grill 2026-07-07 §2.3):**
- **Obsidian** — SAF-запись .md в папку vault (primary) + `obsidian://new` URI (fallback); формат и идемпотентность — [`specs/_contracts/obsidian-note-format.md`](../_contracts/obsidian-note-format.md).

## Non-scope
- Notion/Google Cal/Todoist/Я.Календарь — это **MVP-0.x** fast-follow.

## Контракт / интерфейсы
- **Архитектура — экспортёры из inbox (ADR-015):** destination — не терминальная доставка, а асинхронный экспорт из локального inbox-стора (F1); провал экспорта не теряет мысль.
- `DestinationAdapter` интерфейс: `fix(thought, cta) → FixationResult {status: delivered|failed, ref}`.
- Реализации: ObsidianAdapter, CalendarAdapter, TelegramAdapter, ShareAdapter (inbox — не адаптер, мысль уже персистирована).
- Retry-очередь экспортов в inbox-сторе; **идемпотентность по `thought_id`** (повтор не создаёт дубль в destination).
- **Парсинг времени CTA — не в этой фазе:** LLM (промпт `structure_v1`) отдаёт готовое RFC3339 в `suggested_cta.when` ([`thought.schema.json`](../_contracts/thought.schema.json)); F3 только потребляет, отдельного парсера времени нет.

## Acceptance criteria (EARS)
- **MVP0-F3-AC1** — WHEN пользователь подтверждает предложенное действие, THE SYSTEM SHALL создать запись в выбранном destination и подтвердить голосом/уведомлением.
- **MVP0-F3-AC2** — WHEN мысль типа `reminder` с временем, THE SYSTEM SHALL создать событие/напоминание с корректным временем (из CTA, напр. «завтра 9:00»).
- **MVP0-F3-AC3** — IF целевая интеграция недоступна, THEN THE SYSTEM SHALL оставить мысль в inbox (она уже персистирована, ADR-015) и пометить экспорт «не доставлено» в retry-очереди.
- **MVP0-F3-AC4** — WHEN повторная отправка успешна, THE SYSTEM SHALL снять метку «не доставлено» и подтвердить.

## Edge-cases / unhappy-path
- Obsidian vault не настроен → fallback на Share/Markdown + подсказка настроить.
- Нет прав на календарь → запросить в контексте, не падать.
- Telegram не привязан → пропустить как destination, не блокировать другие.
- Дубль-подтверждение → идемпотентность (не создавать две записи).

## Test plan
- Unit: каждый адаптер + очередь повторной отправки.
- Integration: end-to-end Thought→destination (мок внешних).
- **Adversarial (wave-гейт):** оборвать доступность destination в момент доставки → проверить AC3/AC4.
- Instrumented: реальный Android Calendar/Share на ≥2 OEM.

## Data / privacy
Локальная очередь недоставленных — шифрованная; ПД в RF-облаке при sync (F5).

## Model hints
- Адаптеры/Intents/URI — T2/T3 (Calendar/Obsidian SAF — нативные нюансы).
- Retry-очередь + идемпотентность по thought_id — T3 (на стыке с F1-стором).
