# 03 — HANDOFF (передача работы между ролями)

> Лёгкий аналог CloudEvents. Цель: машинная строгость компактным шаблоном, а не 36 JSON-дефами. Полный шаблон — [`../../.claude/agents/_shared/handoff-template.md`](../../.claude/agents/_shared/handoff-template.md).

## Куда и как

Файл `.planning/_handoffs/<phase>/<NN>-<from>-to-<to>.md` (NN — порядковый). YAML-фронтматтер + короткое тело.

```yaml
---
event: code.commit          # см. conventions §6
from: android-engineer
to: reviewer
phase: MVP0-F1
task: MVP0-F1-capture-service
status: done                # done | blocked | needs-revision
revision_cycle: 0           # 0..3
deliverables:
  - app/.../CaptureService.kt
  - app/.../CaptureServiceTest.kt
adr_refs: [ADR-002, ADR-009]
acceptance_refs: [MVP0-F1-AC1, MVP0-F1-AC2]
cost: { model: opus, tokens_in: 8200, tokens_out: 3100 }
learned: "MIUI глушит FGS без battery-unrestricted — добавлен onboarding-чек."  # → memory-curator решает, индексировать ли
next: "ревью mic-FGS + MediaSession; проверить screen-off на ≥2 OEM"
---
Короткое тело: что сделано, что НЕ сделано, известные риски. 5–10 строк, без пересказа кода.
```

## Контракт

1. **Получатель валидирует** фронтматтер (обязательные поля: `event,from,to,phase,status`). Невалидно → эмить `handoff.error` отправителю, не начинать работу.
2. **`deliverables` — пути, не содержимое.** Получатель открывает по ссылке.
3. **`learned` — кандидат в память**, не сама память. Консолидирует `memory-curator`.
4. **`cost` обязателен** на implementation/review-хендофах — питает cost-audit.
5. **`status: blocked`** требует `blocker_type` (см. [`05-ESCALATION.md`](05-ESCALATION.md)).

## Типовые переходы

| from → to | event | ключевой payload |
|---|---|---|
| planner → impl | `plan.task` | task, deliverables (ожидаемые), depends_on, model_hint |
| impl → reviewer | `code.commit` | commit_sha, files_changed, tests_added |
| reviewer → impl | `review.revision` | required_changes[], severity |
| reviewer/verifier → planner | `acceptance.failed` | failed_criteria_ids[], evidence |
| verifier → architect | `phase.complete` | deliverables_status[], metrics_snapshot |
| architect → memory-curator | `audit.report` | verdict, findings, followups |
| memory-curator → founder | `gate.updated` | gate_id, status, awaiting_founder[] |

## Восстановление и продолжение между сессиями

Хендофы — durable (git). Если сессия упала **или исчерпала контекстное окно**, новая сессия **резюмирует** фазу: читает `STATUS` + `PLAN-<PHASE>` + последний хендоф из `_handoffs/<phase>/` + уже собранный evidence-бандл, и продолжает **с той же стадии** — без рестарта, без пропуска стадий, без выдумывания evidence (ADR-010). Поэтому хендоф **самодостаточен**.

**Чекпойнт-дисциплина:** перед паузой / исчерпанием контекста агент обязан оставить актуальный хендоф-чекпойнт (что сделано, что нет, на какой стадии, что дальше). Одну фазу можно вести в несколько сессий **последовательно** — качество результата и следование дисциплине важнее числа сессий. **Параллельно** одну фазу из двух сессий вести нельзя (гонка за состояние).
