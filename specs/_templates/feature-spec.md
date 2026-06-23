---
phase: <WAVE>-<F|M><n>            # напр. MVP0-F1
title: <название фазы>
wave: <wave-id>
status: draft                     # draft | approved | in-progress | done
tier: 3                           # 1..5 (см. conventions §5)
pipeline: fullstack               # android | backend | fullstack
deps: []                          # фазы/спайки, от которых зависит (напр. [WAVE0-S1])
adr_refs: []                      # релевантные ADR
ui_spec: false                    # true → подключается designer
prd_refs: []                      # секции PRD, на которые опирается
---

# <PHASE> — <название>

## User story
Как `<персона>`, я хочу `<действие>`, чтобы `<ценность>`.

## Scope (в этой фазе)
- …

## Non-scope (НЕ в этой фазе)
- …

## Контракт / интерфейсы
> Для fullstack — контракт API между Android и backend фиксируется ЗДЕСЬ до fork.
- Эндпоинты / модели данных / события …

## Acceptance criteria (EARS)
> Каждый — проверяем verifier'ом как тест. id = `<PHASE>-AC<n>`.

- **<PHASE>-AC1** — WHEN `<триггер>`, THE SYSTEM SHALL `<поведение>`.
- **<PHASE>-AC2** — WHILE `<состояние>`, THE SYSTEM SHALL `<поведение>`.
- **<PHASE>-AC3** — IF `<условие>`, THEN THE SYSTEM SHALL `<поведение>`.

## Edge-cases / unhappy-path
- …

## Test plan
- Unit: …
- Integration / contract: …
- Instrumented (device-матрица, если нативное): …
- (Если промпт/модель) golden + adversarial для evaluator: …

## Evidence & live-gold plan (ADR-010)
> Что агент прогонит САМ до PR и какое доказательство ляжет в `specs/<wave>/evidence/<PHASE>/`. См. [`07-VERIFICATION-EVIDENCE.md`](../../.planning/agent-handbook/07-VERIFICATION-EVIDENCE.md).
- **Self-run:** какие команды (lint/typecheck/unit/integration) → `selftest-*.txt`.
- **Live-gold:** какие сценарии против РЕАЛЬНЫХ сервисов/устройств (live STT/LLM, RuStore sandbox, FTL OEM-матрица) → `live-gold-*.json` / `ftl-runs.md`.
- **Если live невозможен:** что объявляем `evidence_gap` (причина + что нужно: creds/device/sandbox).

## Data / privacy
- Какие ПД трогаем; где хранятся (RF-облако, ADR-004); согласия.

## Model hints (planner ориентир)
- task'и: какие T3 (нативное/security), какие T2 (идиоматичное).
