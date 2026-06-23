# 04 — POST-AUDIT (всегда; phase-close + adversarial wave-gate)

> Фаза **не закрывается** без аудита. Это явное требование операционной модели. Owner — `architect`.

## Уровень 1 — Phase-close audit (каждая фаза, обязательно)

`architect` собирает `specs/<wave>/AUDIT-<PHASE>.md` из 7 линз. Источники — хендофы фазы, diff, тест/девайс-репорты `verifier`, находки ревьюеров.

| # | Линза | Что проверяет | Источник |
|---|---|---|---|
| 1 | code-review | корректность, идиоматичность, дубли | reviewer + diff |
| 2 | security | секреты, PII, ФЗ-152/242, billing | reviewer-security |
| 3 | test-adequacy | покрытие новых веток ≥70% (≥85% security-critical), EARS→тест 1:1 | verifier |
| 4 | ADR-conformance | код не нарушает referenced ADR; нужен ли новый ADR | architect |
| 5 | compliance | согласие на запись, квоты/овередж, чеки 54-ФЗ (где применимо) | reviewer-security |
| 6 | **device-reliability** | нативная приёмка реально прошла на OEM-матрице (FTL + физ-OEM где требуется) | verifier |
| 7 | **cost-audit** | токены/$ фазы по ролям vs `cost-budget.yaml`; не пробили ли cap | memory-curator (метрики) |

**Вердикт:** `pass` / `pass-with-followups` (followups → OPEN-QUESTIONS или новые tasks) / `fail` → revision-loop ([`05-ESCALATION.md`](05-ESCALATION.md)).

Шаблон вердикта — фронтматтер:
```yaml
---
audit: MVP0-F1
verdict: pass-with-followups
lenses: { code: pass, security: pass, tests: pass, adr: pass, compliance: n/a, device: pass, cost: pass }
followups: ["MVP0-F1-followup-xiaomi-retest"]
findings_by_severity: { info: 2, minor: 1, major: 0, critical: 0 }
---
```

## Уровень 2 — Adversarial wave-gate audit (на стыке волн, глубже)

Перед переходом Wave N → N+1, дополнительно к phase-аудитам:

1. **Кросс-фазный инвариант-аудит** (`architect`): не разъехались ли контракты/ADR между фазами волны.
2. **Adversarial-фальсификация** (`verifier` + `native-spike-debugger`): не подтверждаем приёмку, а **пытаемся сломать**. Примеры:
   - S1 «mic-FGS переживает screen-off» — проверить на **втором** OEM, в роуминге, при low-RAM, с Doze.
   - F3 «фиксация в destination» — оборвать сеть в момент доставки; убедиться, что мысль помечена «не доставлено» и переотправляется.
   - F2-структурирование — adversarial-промпты (шум, мат, два спикера, смешанный язык) против golden dataset.
3. **Go/No-Go** по метрикам волны (ADR-008): гейт открывается, только если hard-пороги взяты.

## Связка с памятью

Все находки аудита → `memory-curator` → `memory/<domain>.md` + AgentDB + JOURNAL. Так «пост-аудит» и «обновление памяти» — один поток, а не две задачи.
