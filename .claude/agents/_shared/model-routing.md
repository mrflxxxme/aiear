# Model routing — политика выбора модели

> Решение грилла: **Opus-дефолт + Sonnet-fallback**. Экономия токенов идёт от контекст-дисциплины + эфемерности + выноса механики в T0/T1, а не от понижения дефолта.

## Тиры

| Tier | Движок | Что исполняет | Стоимость |
|---|---|---|---|
| **T0** | детерминизм (Edit/скрипты, Agent Booster) | формат, пины версий, dep-bump, codemod, перемещение файлов, бойлерплейт из шаблона — **без агента** | $0 |
| **T1** | Haiku | STATUS/JOURNAL/memory-записи, changelog, doc-string, простые тест-скелеты, форматирование хендофов | ~$0.0002 |
| **T2** | Sonnet | идиоматичная имплементация (Compose-CRUD, FastAPI-эндпоинт, Pydantic, обычные тесты), общий код-ревью, designer-моки, evaluator-харнесс | ~$0.003 |
| **T3** | **Opus (дефолт)** | planner-декомпозиция, architect/ADR, нативная BT/FGS/OEM-логика, reviewer-security, verifier-гейт, evaluator-вердикт, native-spike-debugger | ~$0.015 |

## Pinned-Opus (никогда не падают на дешёвую модель)

`reviewer-security` · `verifier` (решение гейта) · `architect` (ADR/аудит) · `planner`.
Причина: решения, которые дорого испортить. Зелёный CI их не заменяет.

## Триггеры

**Эскалация (на Opus / удержать Opus):**
- `revision_cycle >= 2` на одном task (дешёвая модель застряла),
- reviewer пометил `major`/`critical`,
- task трогает `native` / `security` / `billing` / `migration`.

**Fallback Opus → Sonnet (для НЕ-pinned ролей):**
- пробит `per_task.soft_cap` в `cost-budget.yaml`,
- `model_hint` = T2 и `complexity_score < 0.3`,
- роль ∈ {reviewer, designer, evaluator-харнесс, memory-curator}.

## Как planner проставляет хинт

В `PLAN-<PHASE>.md` каждый task получает:
```yaml
- task: MVP0-F1-capture-service
  role: android-engineer
  model_hint: T3        # нативный mic-FGS — сложно
  complexity: 0.7
- task: MVP0-F1-history-list-ui
  role: android-engineer
  model_hint: T2        # обычный Compose-список
  complexity: 0.2
```

Роутер (или агент-исполнитель) применяет хинт + триггеры. При сомнении для нативного/security — поднимай тир, не опускай.
