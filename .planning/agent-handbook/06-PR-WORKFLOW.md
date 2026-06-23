# 06 — PR WORKFLOW (мерж, тиры, гейт фаундера)

> 1 PR на фазу. Фаундер — единственный аппрувер tier 3+. ИИ-агенты не мержат tier 3+.

## Жизненный цикл

```
phase-ветка (phase/<PHASE>-<slug>)
   → коммиты (conventional, атомарные)
   → self-run + live-gold evidence-бандл собран (ADR-010)
   → verifier зелёный + architect AUDIT verdict ∈ {pass, pass-with-followups}
   → memory-curator открывает PR (тело из шаблона) + обновляет STATUS/README
   → CI-гейты (см. ниже)
   → фаундер: tier 1-2 авто/ack · tier 3-4 явный аппрув · tier 5 в сессии
   → merge → main → ветка удаляется
```

## CI-гейты (GitHub Actions)

```
1. Lint            ktlint/detekt (Android) · ruff (backend)
2. Type-check      mypy --strict (backend); kotlinc warnings-as-errors
3. Unit + coverage ≥70% новый код / ≥85% security-critical
4. Instrumented    Gradle Managed Devices → Firebase Test Lab (матрица OEM)
5. Screenshot      Compose screenshot-тесты
6. Security        Semgrep/Bandit · gitleaks · pip-audit/npm audit
7. Golden regress  если менялся промпт-артефакт → evaluator
8. Evidence-чек    PR без секции Evidence / без бандла specs/<wave>/evidence/<PHASE>/ → block (ADR-010)
```

**Обработка фейла CI:** tier 1 — нет merge, revision-loop. tier 2+ — block, reviewer ищет причину. Security-чек красный — emergency stop, reviewer-security.

## Тиры (повтор из conventions §5)

| Tier | Что | Действие фаундера |
|---|---|---|
| 1 | доки/формат/dep-patch | авто-merge на зелёном CI |
| 2 | тесты/рефактор/copy | ack |
| 3 | новый экран/эндпоинт/фича | **явный аппрув** |
| 4 | архитектура/security/billing/ФЗ/миграции/нативный фон | **явный аппрув + ADR-линк** |
| 5 | хотфикс | аппрув в сессии |

## Шаблон тела PR

```markdown
## <PHASE> — <название фазы>
**Tier:** 3 · **ADR:** ADR-002, ADR-009 · **Wave:** Wave 0

### Что сделано
- EARS-критерии: MVP0-F1-AC1..AC4 — все ✅ (verifier evidence: <ссылка>)

### Аудит (phase-close)
verdict: pass-with-followups · followups: [MVP0-F1-followup-xiaomi-retest]
линзы: code ✅ security ✅ tests ✅ adr ✅ compliance n/a device ✅ live-gold ✅ cost ✅

### Девайс
FTL: Pixel 8 ✅, Galaxy S23 ✅ · физ-OEM: Xiaomi — followup

### Evidence (ADR-010)
self-run ✅ (specs/<wave>/evidence/<PHASE>/selftest-*.txt) · live-gold ✅ STT/LLM live pass-rate / WER (live-gold-*.json) · FTL-прогоны (ftl-runs.md)
evidence_gap: <нет | причина + что нужно (creds/device/sandbox)>

### Cost
фаза: $0.41 (planner 0.12 / android 0.21 / review 0.05 / verify 0.03) — в бюджете

### Чек фаундера
- [ ] EARS выполнены (подтверждено evidence, не утверждением)
- [ ] evidence-бандл приложен (self-run + live-gold); gap'ы явные
- [ ] нет секретов / ПД-нарушений
- [ ] ADR соблюдены
```

## Фаундер-in-the-loop (2 UX-паттерна)

- **(a) GitHub UI** — ревью PR, merge (default).
- **(b) В Claude Code** — автономная сессия возвращает артефакт + сводку; фаундер говорит «merge / revise X / abort».

## Автономные сессии

Фаундер авторизовал диспетч отдельных фаз независимыми автономными сессиями. Каждая такая сессия:
1. берёт ОДНУ фазу (не пересекается с другими по `phase-state`),
2. гонит пайплайн до phase-аудита,
3. открывает PR и **останавливается на гейте** — мерж только фаундер (tier 3+).
