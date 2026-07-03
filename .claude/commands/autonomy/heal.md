---
description: Self-healing — детект красного main, авто-реверт offending-мёржа, автономный fix-loop (ADR-011 D7)
argument-hint: [check | revert <sha> | fix <sha>] (default: check → полный протокол)
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent, TaskCreate, TaskUpdate, ToolSearch
---

# /autonomy:heal — авто-реверт + автономный fix-loop (ADR-011 D7)

Main должен быть **зелёным по построению**: пост-мёрж регрессия ревертится первой, фиксится второй. Разработка продолжается; fix-loop автономен; фаундер ВСЕГДА уведомляется о реверте. Режим: **$ARGUMENTS** (default = полный протокол).

> Под rails-first (auto-merge off) регрессии редки (мёржи ручные-через-ack), но протокол вооружён на случай пост-мёрж красного main.

## 1. Детект
`python scripts/autonomy/check_main_health.py` → exit 0 = healthy (готово, рапорт); exit 20 = JSON-verdict со списком failing workflows + `offender_sha` (squash-мёрж-коммит, чей head тестировал failing-run); exit 1 = не могу судить → RUN-QUEUE `stuck` + notify, СТОП (не мёржи вслепую).

Перед revert — sanity-check атрибуции: `git log origin/main --oneline -5` — подтверди, что `offender_sha` — недавний runner-era мёрж-коммит; если фейл предшествует новейшим мёржам или sha не на main → СТОП → RUN-QUEUE `stuck` + verdict JSON + notify (мис-атрибуция хуже паузы).

## 2. Revert (main зелёный по построению)
1. `git fetch origin main` → ветка `claude/revert-<sha7>` от `origin/main`.
2. `git revert --no-edit <offender_sha>` (squash-мёржи — обычные коммиты; прямой revert; никогда `--force` на main).
3. Конфликт revert'а (позднейший мёрж поверх) → НЕ решай творчески — RUN-QUEUE `stuck` (details: конфликтующие пути) + notify, СТОП.
4. PR (`revert: <original title>` + линк failing-run URL) → `gh pr checks --watch` → merge. Revert-PR всё равно gated (ci-evidence/ci-security required) — это намеренно.
5. **Notify (обязательно, D7):** RUN-QUEUE `revert` (offender sha/PR, failing workflows, fix-ветка) + PushNotification + Telegram (`notify.json`). Фаундер узнаёт о каждом реверте в момент, когда он случился.

## 3. Fix-loop (автономный)
1. Ветка `claude/fix-<sha7>` от свежего `origin/main`; `git cherry-pick <offender_sha>` (механические конфликты решай против revert-коммита; семантические → `stuck` + notify).
2. Диагноз из логов failing-run (`gh run view <run_id> --log-failed`) — фикси реальную регрессию, добавь/поправь тест, который ДОЛЖЕН был поймать (регрессия, пережившая гейты = gate-gap; закрой). Нативная регрессия фона → спавни `native-spike-debugger` (agent-device-петля).
3. Полные локальные гейты (`./gradlew ...` / `ruff+mypy+pytest` + evidence-гейты + device_survival заново на новом HEAD) → PR → чеки зелёные → tripwire classify → merge на exit 0, или RUN-QUEUE `ack-needed` на exit 10.
4. Max **3 fix-цикла**: всё ещё красный после 3 → RUN-QUEUE `stuck` + полный диагноз + notify, оставь ветку фаундеру.
5. Успех: резолв в RUN-QUEUE (`complete`: revert PR + fix PR + root-cause одной строкой) + `log_decision.py --kind impl --fork "regression root-cause" ...`.

## Guardrails
- ОДИН offender за раз: несколько workflow фейлят на разных sha → реверть НОВЕЙШИЙ первым, ре-чек health, итерируй.
- Никогда не реверть founder-authored (не-runner) коммит без явного ask — `stuck` + notify.
- Никогда force-push, никогда байпас хуков, никогда не решай revert-конфликт выбрасыванием чужой работы.
- В окружении без `gh` — GitHub MCP-эквиваленты (`create_pull_request`/`merge_pull_request`/`get_job_logs`).
