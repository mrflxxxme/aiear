# specs/ — спецификации (агент-исполнимое ТЗ)

> Spec — первичный артефакт. Из неё генерятся код + тесты + доки. Критерии приёмки — **EARS** (см. [`_meta/conventions.md`](../.planning/_meta/conventions.md) §3), чтобы агенту нечего было додумывать.

## Структура

```
specs/
  _templates/
    feature-spec.md   # шаблон фичевой фазы (MVP-0 F*, MVP-1 M*)
    spike-spec.md     # шаблон спайка риска (Wave 0 S*)
  wave-0/             # S1–S6 (spike-спеки) — БЛОКИРУЮЩИЕ
  mvp-0/              # F1–F7 (feature-спеки) — детально
  mvp-1/ v1/ v2/      # стабы (раскрывает planner JIT перед волной)
```

Сопутствующие файлы фазы (создаёт пайплайн): `PLAN-<PHASE>.md` (planner), `AUDIT-<PHASE>.md` (architect), `_handoffs/<phase>/` (роли).

## Поток

1. Spec фазы готов и **аппрувнут фаундером** (tier 3+).
2. `planner` читает spec → `PLAN-<PHASE>.md`.
3. Пайплайн исполняет ([`02-PIPELINE.md`](../.planning/agent-handbook/02-PIPELINE.md)).
4. `verifier` **прогоняет** каждый EARS-критерий как тест + live-gold, собирает evidence-бандл (ADR-010).
5. `architect` закрывает phase-аудитом; `memory-curator` обновляет состояние + PR.

## Правила написания

- Не дублируй PRD — **ссылайся** на секции [`docs/EARAI-PRD.md`](../docs/EARAI-PRD.md).
- Каждый EARS-критерий получает id `<PHASE>-AC<n>` и обязан быть проверяем.
- Указывай `tier`, `pipeline`, `deps`, `model_hint`-подсказки, `ui-spec:` (если UI).
- Edge-cases и unhappy-path — обязательны (агент не должен их «придумывать»).
- **Evidence & live-gold plan (ADR-010)** — обязателен: что прогоняется САМ до PR и какое доказательство (self-run + live-gold) ляжет в `specs/<wave>/evidence/<PHASE>/`. См. [`07-VERIFICATION-EVIDENCE.md`](../.planning/agent-handbook/07-VERIFICATION-EVIDENCE.md).
