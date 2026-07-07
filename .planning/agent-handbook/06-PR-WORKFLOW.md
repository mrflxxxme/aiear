# 06 — PR WORKFLOW (мерж, тиры, гейт фаундера)

> 1 PR на фазу. Фаундер — единственный аппрувер tier 3+. ИИ-агенты не мержат tier 3+.

## Жизненный цикл

```
phase-ветка (`claude/*` — каноническая конвенция A8, conventions §7; `phase/*`/`wave/*` — deprecated)
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

### CI-дисциплина: довести до зелёного (уроки WAVE0-S1)

Фаза не закрывается с красным CI. Открыла сессия PR — доводит чеки до зелёного
(re-diagnose → re-kick каждый раунд), либо явно репортит реальный блокер (не молчит).
Частые **startup_failure**'ы GitHub Actions (run с **0 jobs**, показан по пути файла
`.github/workflows/*.yml`, event=push = невалидный workflow-файл, а не баг кода):

- **`run: echo "...: ..."`** — двоеточие-пробел в **неэкранированном** скаляре = битый YAML.
  Оборачивай: `run: 'echo "x: y"'` или блок `run: |`.
- **`secrets` в `if:`** — запрещённый контекст → startup_failure. Мапь в `env:` и проверяй
  `env.X`: `env: { TOKEN: ${{ secrets.X }} }` + `if: ${{ env.TOKEN != '' }}`.
- **`hashFiles()` в `if:` на уровне JOB** — вычисляется до checkout → startup_failure. Гейти на
  уровне STEP (после checkout) или path-фильтром `on.pull_request.paths`.
- **gitleaks-action@v2** требует `GITHUB_TOKEN` для PR-сканов — передай через `env`.
- **path-фильтр и head**: workflow с `paths:` не запустится, если последний коммит не трогает эти
  пути → у head не будет его чека. Делай коммит, меняющий нужный путь, последним.

Перед пушем workflow-правок валидируй локально (`python3 -c "import yaml; yaml.safe_load(open('f'))"`):
YAML-синтаксис ловится, но expression-ошибки (`secrets`/`hashFiles` в `if`) — нет, их выдаёт только
запуск → проверяй по `get_job_logs`/`get_workflow_run` (0 jobs = startup_failure, ищи невалидную конструкцию).

## Тиры (повтор из conventions §5)

| Tier | Что | Действие фаундера |
|---|---|---|
| 1 | доки/формат/dep-patch | авто-merge на зелёном CI |
| 2 | тесты/рефактор/copy | ack |
| 3 | новый экран/эндпоинт/фича | **явный аппрув** |
| 4 | архитектура/security/billing/ФЗ/миграции/нативный фон | **явный аппрув + ADR-линк** |
| 5 | хотфикс | аппрув в сессии |

> ⚠️ **Rails-first (ADR-011 D1):** до активации auto-merge фаундером **все тиры (вкл. 1–2) проходят через founder-ack** — «авто-merge»/«ack» tier 1–2 приостановлены и описывают целевое состояние после активации (Wave-0-green + CI-secrets + рабочий evidence-контур, grill 2026-07-07 / 4.4).

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
