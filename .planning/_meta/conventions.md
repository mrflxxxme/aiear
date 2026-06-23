# Конвенции AIEAR (naming, форматы, идентификаторы)

> Единый словарь форм. Все агенты обязаны соблюдать. Нарушение — block PR (проверяет `reviewer`).

## 1. Идентификаторы

| Сущность | Формат | Пример |
|---|---|---|
| Wave | `wave-<n>` / human-имя | `wave-0`, `mvp-0`, `mvp-1`, `v1`, `v2` |
| Phase | `<WAVE>-<FEATURE>` | `MVP0-F1`, `WAVE0-S1`, `MVP1-M2` |
| Task | `<PHASE>-<slug>` | `MVP0-F1-capture-service` |
| ADR | `ADR-<3 цифры>` | `ADR-001` |
| Risk | `R-<2-4 цифры>` | `R-31` |
| Spike | `S<n>` | `S1`…`S6` |
| Feature (MVP-0) | `F<n>` | `F1`…`F7` |
| Feature (MVP-1) | `M<n>` | `M1`…`M5` |

## 2. Файлы и директории

```
docs/                     # PRD + ресёрч (источник продуктовой истины)
.planning/
  PROJECT.md STATUS.md JOURNAL.md OPEN-QUESTIONS.md
  _meta/                  # conventions, glossary, stack
  agent-handbook/         # как работает харнесс
  decisions/ADR-*.md      # ADR
  roadmap/<wave>.md       # волны + gate-критерии
  memory/<domain>.md      # durable уроки (индексируются в AgentDB)
  _handoffs/<phase>/*.md  # лёгкие хендоф-артефакты (эфемерные)
specs/
  _templates/             # feature-spec.md, spike-spec.md
  <wave>/<PHASE>.md        # spec фазы (EARS)
  <wave>/PLAN-<PHASE>.md   # план фазы (planner)
  <wave>/AUDIT-<PHASE>.md  # phase-close аудит
  <wave>/evidence/<PHASE>/ # evidence-бандл (ADR-010): self-run, live-gold, FTL, coverage
.claude/agents/           # native-субагенты (1 файл = 1 роль)
app/                      # Android (Kotlin) — появится в MVP-0
backend/                  # FastAPI — появится в MVP-0
.github/workflows/        # CI
```

- Имена файлов спек — `kebab-case`, без пробелов. Прозовые доки — на русском, идентификаторы/код — на английском.
- Один durable-факт = одна секция в `memory/<domain>.md` с датой и источником.

## 3. EARS — формат критериев приёмки

Каждый критерий — одна из 5 форм (агенту нечего додумывать):

| Форма | Шаблон |
|---|---|
| Ubiquitous | THE SYSTEM SHALL `<поведение>` |
| Event | **WHEN** `<триггер>`, THE SYSTEM SHALL `<поведение>` |
| State | **WHILE** `<состояние>`, THE SYSTEM SHALL `<поведение>` |
| Optional | **WHERE** `<фича включена>`, THE SYSTEM SHALL `<поведение>` |
| Unwanted | **IF** `<условие>`, **THEN** THE SYSTEM SHALL `<поведение>` |

Каждый EARS-критерий получает id `<PHASE>-AC<n>` и должен быть проверяем `verifier`'ом как тест.

## 4. ADR-формат

Файл `decisions/ADR-NNN-<slug>.md`, фронтматтер:
```yaml
---
id: ADR-001
title: Android-first
status: accepted        # proposed | accepted | superseded
date: 2026-06-23
supersedes: []
informs: [WAVE0, MVP0]
deciders: [founder]
---
```
Тело: **Контекст → Решение → Последствия → Альтернативы (отклонены)**.

## 5. Тиры одобрения фаундера

| Tier | Что | Действие |
|---|---|---|
| 1 | доки/формат/dep-patch | авто-merge на зелёном CI |
| 2 | тесты/рефактор/copy | скользящий взгляд, ack |
| 3 | новый экран/эндпоинт/фича | явный аппрув на фаза-гейте |
| 4 | архитектура/security/billing/ФЗ-152-242/миграции/нативный фон | явный аппрув + ADR-линк |
| 5 | хотфикс | аппрув в той же сессии |

ИИ-агенты **не имеют merge-прерогативы** на tier 3+. Зелёный CI + аппрув ревьюеров + **evidence-бандл (ADR-010)** — необходимо, но не достаточно.

## 6. Хендоф-события (тип в YAML-поле `event`)

Лёгкий аналог CloudEvents — компактные имена `<context>.<event>`:
`plan.task` · `code.commit` · `review.report` · `review.revision` · `phase.complete` · `gate.updated` · `acceptance.failed` · `task.unclear` · `spec.incomplete` · `escalation.*` · `cost.soft-cap` · `cost.hard-cap` · `agent.stagnated` · `audit.report` · `memory.updated`.

Схема полей — [`.claude/agents/_shared/handoff-template.md`](../../.claude/agents/_shared/handoff-template.md).

## 7. Git / commit

- Trunk-based, короткие ветки `phase/<PHASE>-<slug>` или `wave/<wave>-<topic>`.
- Conventional commits: `feat(MVP0-F1): ...`, `fix(...)`, `docs(...)`, `chore(...)`, `test(...)`.
- 1 PR на фазу. Тело PR — из шаблона [`06-PR-WORKFLOW.md`](../agent-handbook/06-PR-WORKFLOW.md).
