---
phase: MVP0-F3
title: Фиксация в действие (интеграции-ядро)
wave: mvp-0
status: approved
tier: 4
pipeline: fullstack
deps: [MVP0-F2]
adr_refs: [ADR-004, ADR-007]
ui_spec: true
prd_refs: ["§8 MVP-0 F3", "§5"]
---

# MVP0-F3 — Фиксация в действие

## User story
Как пользователь, я хочу, чтобы структурированная мысль **сама легла** в мою систему (Obsidian / календарь / Telegram), потому что пойманная мысль без действия = потерянная мысль.

## Scope (destinations-ядро)
- **Obsidian** (запись в vault — через URI/файл).
- **Системный Календарь / Напоминания** (Android Calendar/Intents).
- **Telegram** (отправка себе).
- **Share Sheet / Markdown-экспорт** (catch-all).

## Non-scope
- Notion/Google Cal/Todoist/Я.Календарь — это **MVP-0.x** fast-follow.

## Контракт / интерфейсы
- `DestinationAdapter` интерфейс: `fix(thought, cta) → FixationResult {status: delivered|failed, ref}`.
- Реализации: ObsidianAdapter, CalendarAdapter, TelegramAdapter, ShareAdapter.
- Очередь повторной отправки (для AC3).

## Acceptance criteria (EARS)
- **MVP0-F3-AC1** — WHEN пользователь подтверждает предложенное действие, THE SYSTEM SHALL создать запись в выбранном destination и подтвердить голосом/уведомлением.
- **MVP0-F3-AC2** — WHEN мысль типа `reminder` с временем, THE SYSTEM SHALL создать событие/напоминание с корректным временем (из CTA, напр. «завтра 9:00»).
- **MVP0-F3-AC3** — IF целевая интеграция недоступна, THEN THE SYSTEM SHALL сохранить мысль локально и пометить как «не доставлено» для повторной отправки.
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
- Адаптеры/Intents/URI — T2/T3 (Calendar/Obsidian — нативные нюансы).
- Парсинг времени из CTA — T3 (на стыке с F2).
