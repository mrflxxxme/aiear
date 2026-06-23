# Handoff template — компактная схема передачи

> Лёгкий аналог CloudEvents. Машинная строгость — фронтматтером, не 36 JSON-дефами. Механика — [`../../../.planning/agent-handbook/03-HANDOFF.md`](../../../.planning/agent-handbook/03-HANDOFF.md).

## Обязательные поля

| Поле | Тип | Обяз. | Заметка |
|---|---|---|---|
| `event` | enum | ✅ | см. conventions §6 |
| `from` | role | ✅ | роль-отправитель |
| `to` | role \| [role] | ✅ | массив = fan-out (параллельные ревьюеры) |
| `phase` | id | ✅ | `MVP0-F1` |
| `status` | enum | ✅ | `done` \| `blocked` \| `needs-revision` |
| `task` | id | при task-работе | |
| `revision_cycle` | 0..3 | ✅ | хранит счётчик циклов |
| `deliverables` | [path] | при `done` | **пути, не содержимое** |
| `adr_refs` | [id] | при impl | какие ADR соблюдены |
| `acceptance_refs` | [id] | при verify | какие EARS закрыты |
| `cost` | obj | при impl/review | `{model, tokens_in, tokens_out}` |
| `evidence` | [path] | при impl/verify/eval | пути в `specs/<wave>/evidence/<PHASE>/` (self-run, live-gold, FTL, coverage) — ADR-010 |
| `evidence_gap` | string | если live невозможен | причина + что нужно (creds/device/sandbox); **не тихий скип** |
| `learned` | string | опц. | кандидат в память (решает curator) |
| `blocker_type` | enum | при `blocked` | missing_dependency \| unclear_spec \| external_failure \| conflicting_review \| cost_cap_breach \| native_oem \| needs-live-evidence \| other |
| `next` | string | ✅ | что делать получателю |

## Канонический пример

```yaml
---
event: code.commit
from: backend-engineer
to: [reviewer, reviewer-security]
phase: MVP1-M5
task: MVP1-M5-rustore-recurrent
status: done
revision_cycle: 0
deliverables:
  - backend/app/billing/rustore_pay.py
  - backend/tests/billing/test_rustore_pay.py
adr_refs: [ADR-005]
acceptance_refs: [MVP1-M5-AC1, MVP1-M5-AC2]
cost: { model: opus, tokens_in: 9100, tokens_out: 2700 }
evidence:
  - specs/mvp-1/evidence/MVP1-M5/selftest-backend.txt
  - specs/mvp-1/evidence/MVP1-M5/live-gold-rustore-sandbox.json
evidence_gap: "live-продление требует прод-merchant — followup; sandbox-рекуррент зелёный"
learned: "RuStore Pay sandbox требует отдельный merchant-id на рекуррент — задокументировано."
next: "security: проверить, что ключ эквайера не в коде; review: идемпотентность вебхука продления"
---
Реализован рекуррентный платёж через RuStore Pay SDK + вебхук продления.
НЕ сделано: овередж-пакеты (отдельный task M5b). Риск: тайминг grace-period не протестирован на реальном продлении (только sandbox).
```

## Валидация получателем

```
1. Есть все обязательные поля для данного event? нет → emit handoff.error
2. status=blocked но нет blocker_type? → handoff.error
3. deliverables существуют по путям? нет → handoff.error
4. impl/verify/eval-хендоф без `evidence` и без `evidence_gap`? → возврат (review.revision) / handoff.error (ADR-010)
5. ок → начать работу, по завершении эмитить свой хендоф
```
