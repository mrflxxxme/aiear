# 02 — PIPELINE (механика фазы)

> Как фаза проходит от spec до merge. Шаблоны выбирает `planner`.

## Канонический поток фазы

```
[spec фазы готов, аппрувнут фаундером]
        │
        ▼
   planner — декомпозирует фазу → PLAN-<PHASE>.md (tasks + DAG + модель-хинты)
        │
        ├──► designer (если в spec есть `ui-spec:`)
        │         │
        │         ▼
        │   android-engineer (UI) ──┐
        ├──► android-engineer (логика) ∥ backend-engineer
        │                            │
        ├────────────────────────────┘
        ▼
   reviewer ∥ reviewer-security   (параллельно)
        │
        ▼
   verifier  (EARS-критерии как тесты + девайс-петля)
        │
        ▼
   architect — phase-close AUDIT-REPORT (8 линз, вкл. live-gold/evidence)   ← всегда, не пропускается
        │
        ▼
   memory-curator — STATUS/JOURNAL/memory + индекс AgentDB + регенерит README
        │
        ▼
   фаундер аппрувит гейт (tier 3+)  →  PR merge → main
```

## Шаблоны пайплайна (выбирает planner)

| Шаблон | Когда | Последовательность |
|---|---|---|
| [`pipeline-backend.yaml`](../../.claude/agents/_shared/pipeline-backend.yaml) | только backend (API/DB/proxy/billing) | planner → backend-engineer → (reviewer ∥ reviewer-security) → verifier → architect → memory-curator |
| [`pipeline-android.yaml`](../../.claude/agents/_shared/pipeline-android.yaml) | только Android (UI/сервисы/нативное) | planner → (designer?) → android-engineer → reviewer → verifier → architect → memory-curator |
| [`pipeline-fullstack.yaml`](../../.claude/agents/_shared/pipeline-fullstack.yaml) | оба слоя (capture-pipeline, billing, sync) | planner → fork: (designer→android) ∥ (backend) → join: (reviewer ∥ reviewer-security) → verifier → architect → memory-curator |

**Spike-пайплайн (Wave 0):** planner → `native-spike-debugger` (или backend для S4/S5) → verifier (валидирует EARS на device-матрице) → architect (go/no-go-запись) → memory-curator. UI/ревью-слой минимален — спайк доказывает выполнимость, не шлёт фичу.

## Выбор модели на task

`planner` ставит каждому task `model_hint` (T1/T2/T3) по [`model-routing.md`](../../.claude/agents/_shared/model-routing.md). Дефолт T3 (Opus), но механику (T0/T1) выноси из LLM.

## Параллелизм

- `reviewer` ∥ `reviewer-security` — всегда параллельно.
- `android-engineer` ∥ `backend-engineer` — параллельно в fullstack-фазе после fork.
- Несколько независимых фаз могут идти **разными автономными сессиями** (фаундер авторизовал) — но одна фаза = одна сессия, чтобы не было гонок за `phase-state`.

## Custom-пайплайн

Для не-фичевой работы (ресёрч, ADR, пост-мортемы) `planner` строит ad-hoc цепочку прямо в PLAN, без YAML-шаблона.
