# 05 — ESCALATION (revision-loop, клапаны, стагнация)

> Когда что-то идёт не так. Цель — не крутить впустую Opus-токены и не дрейфовать.

## Revision-loop (макс 3 цикла)

```
reviewer | verifier нашёл проблему
    │
    ▼  создаёт review.revision (required_changes[])
planner — инкрементальный re-plan (дельта, не полный пере-план)
    │
    ▼
impl — фиксит по revision
    │
    ▼  re-review
    ├── ✅ passed → пайплайн дальше
    └── ❌ failed → cycle++ (хранится в хендофе revision_cycle)
                      │
                      └── после 3 циклов → эскалация фаундеру (escalation.iteration-exhausted)
```

`revision_cycle` — поле в хендофе (0..3). Фаундер может явно поднять лимит, если задача требует глубже.

## Клапаны эскалации

| Ситуация | Событие | Куда |
|---|---|---|
| reviewer ≠ impl по фиксу | `escalation.review-deadlock` | `architect` арбитрит; если мало — фаундер |
| verifier нашёл дыру вне scope spec | `escalation.scope-creep` | `planner` + фаундер: расширить spec или новая фаза |
| security нашёл tier-4 в tier-3 PR | `escalation.security-upgrade` | ре-тиринг + явный аппрув фаундера |
| **нативный/OEM баг, impl буксует** | `escalation.native-stuck` | **`native-spike-debugger`** (agent-device глубокая петля); если мало — фаундер пейрит |
| 3 цикла исчерпаны | `escalation.iteration-exhausted` | фаундер: продолжить/бросить фазу |

## Стагнация (kill-switch)

- Агент без прогресса **>30 мин** wall-clock → авто `agent.stagnated`, сессия останавливается. Применяется к dev-агентам (не к user-traffic). Параметр — `cost-budget.yaml › operational.stagnation_kill_switch_minutes`.

## Cost-эскалация

| Триггер | Действие |
|---|---|
| soft-cap пробит (не-pinned роль) | fallback на Sonnet (`cost.soft-cap`) |
| hard-cap пробит | пайплайн на паузу, явный аппрув фаундера (`cost.hard-cap`) |
| month kill-switch | стоп, policy-review фаундера (не тихий override) |

## Главное правило

**Застрял → эскалируй, не крути.** Лучше отдать фаундеру чистый блокер с контекстом, чем сжечь токены в цикле. Блокер-хендоф обязан содержать `blocker_type` ∈ {missing_dependency, unclear_spec, external_failure, conflicting_review, cost_cap_breach, native_oem, other} и что уже пробовали.
