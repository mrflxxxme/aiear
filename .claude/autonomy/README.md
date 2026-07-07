# `.claude/autonomy/` — конфиг автономного runner'а + предохранители

Source-of-truth автономного многофазного runner'а per [ADR-011](../../.planning/decisions/ADR-011-autonomous-multiphase-runner.md) (адаптация ORIION ADR-037). Под runner'ом **усиленный гейт-стек — merge-authority, а не глаза фаундера** — поэтому эти рельсы должны существовать и быть зелены *до* включения auto-merge (Блок A предшествует передаче merge-authority).

## ⚠️ Текущий режим: RAILS-FIRST — auto-merge ВЫКЛЮЧЕН

По решению фаундера (интеграция 2026-07-03) стек построен, но **auto-merge не активирован**. Runner доводит фазу до зелёного и **ПАУЗИТ на фаундер-ack на КАЖДОМ PR** (как сегодняшние автономные сессии). Auto-merge включается, когда:

1. **Wave 0 спайки S1–S5 зелёные** на ≥2 OEM (главный нативный риск снят реальными доказательствами), и
2. **CI подключён к secrets** (ci-android FTL, ci-backend live, ci-security), и
3. **evidence-контур реально работает** (grill 2026-07-07, решение 4.4): единый путь `specs/<wave>/evidence/<PHASE>/`, машиночитаемый DoD, непустой `manifest.json` обязателен для нативных/AI-фаз, `verify_evidence.py --require` фейлит при его отсутствии.

Тогда фаундер флипает переключатель (см. BUILD-PLAN §Активация). До этого задняя растяжка (D2) всё равно активна как вторая линия.

## Блоки сборки (ADR-011)

Живой трекер: [`BUILD-PLAN.md`](./BUILD-PLAN.md).

| Блок | Что | Статус |
|---|---|---|
| **A — Рельсы** | evidence-схема + `ci-evidence` + tripwire-конфиг + classify + pre-merge хук | ✅ построено |
| **B — Фронт** | escalation-policy + decisions-log + judge-панель | ✅ построено |
| **C — Runner** | `/autonomy:run` + RUN-QUEUE + `/autonomy:ack` + `/autonomy:discuss` + role-loader + notify | ✅ построено |
| **D — Self-healing** | `check_main_health.py` + `/autonomy:heal` (auto-revert → notify → fix-loop) | ✅ построено |
| **E — Параллелизм** | opt-in worktree-треки (max 2, секвенциальные мёржи) | ✅ документирован (run.md) |
| **Активация auto-merge (D1)** | branch protection + hook-arming + флип toggle | ⏸ **отложена** (Wave-0-green + CI-secrets) |

## Файлы здесь

| Файл | Роль |
|---|---|
| `tripwire.yaml` | **D2 задняя растяжка.** Path-глобы 7 категорий, которые НЕ должны auto-merge (миграции · auth/аккаунт · billing · секреты/ключи · ПД/ФЗ-242 · **нативный фон** · публичные контракты). Классификатор сверяет диф PR'а; совпало → RUN-QUEUE + notify + ждать `/ack`. `requires_device_evidence: true` у нативной категории. |
| `evidence-schema.json` | **D3 целостность гейтов.** JSON-Schema local-only-гейт evidence-артефакта (live-gold STT/LLM, RuStore sandbox, device-survival, adversarial). |
| `escalation-policy.md` | **D4 передняя растяжка.** Что агент владеет (all impl+arch, решает+логирует) vs эскалирует (продукт/рынок + трипвайр). Читает `/autonomy:discuss`. |
| `judge-panel.md` | **D5 оптимальность.** Триггер широкого форка + N-подходов + рубрика `evaluator` + победитель/прививка + evidence. |
| `BUILD-PLAN.md` | Живой трекер Блоков A–E + founder-owned toggles активации. |
| `settings.hook-snippet.json` | **D2 hook-конфиг (founder-armed).** Мёржни `hooks`-ключ в `.claude/settings.json`, чтобы вооружить pre-merge tripwire-хук. Claude не может self-install hook-конфиг. |
| `notify.json` *(опц., создаёт фаундер)* | `{"telegram_chat_id": "<id>"}` — включает Telegram phone-ack (D8). Без него: desktop push + RUN-QUEUE. |

Скрипты (`scripts/autonomy/`): `verify_evidence.py` (D3) · `classify_tripwire.py` (D2) · `log_decision.py` (D4) · `run_queue.py` (D8 очередь) · `premerge_hook.py` (D2 хук) · `load_role.py` (спавн-промпт роли) · `check_main_health.py` (D7 regression-watch) · `provision_env.py` (preflight funded `.env`).
Команды: `/autonomy:discuss <phase>` (D4) · `/autonomy:run [queue]` (D6 runner) · `/autonomy:ack [RQ-ID verdict]` (фаундер resolve) · `/autonomy:heal` (D7 auto-revert + fix-loop).

Все скрипты **stdlib-only** (нет зависимости от PyYAML/uv — AIEAR не имеет гарантированного python-venv; хук + CI гоняют их как bare `python`). ASCII-консоль (Windows cp1251-safe), UTF-8 в файлах (русские summary).

## Evidence-протокол (D3) — как фаза доказывает local-only гейт

GitHub CI не может гонять funded live-gold ru-STT/LLM, RuStore sandbox, on-device OEM survival, adversarial audit. Фаза, которая их гоняет, обязана оставить **закоммиченное, привязанное к коммиту доказательство** — **внутри человеческого evidence-бандла ADR-010** (единый путь evidence, grill 2026-07-07 / A5):

1. Гейт-скрипт пишет `specs/<wave>/evidence/<PHASE>/<gate>.json` по `evidence-schema.json`, с `head_sha` = точный коммит прогона и `verdict` = `PASS`/`FAIL`.
2. Фаза объявляет **ВСЕ** гейты своего DoD в `specs/<wave>/evidence/<PHASE>/manifest.json`:
   ```json
   { "phase": "WAVE0-S2", "required_gates": ["device_survival"] }
   ```
3. Workflow `ci-evidence` гоняет `scripts/autonomy/verify_evidence.py` (discovery по `specs/*/evidence/*/manifest.json`), ассертя, что каждый объявленный гейт существует, **свежий** (`head_sha` == PR head), и `PASS`. Иначе мёрж блокируется. Для нативных/AI-фаз runner дополнительно гоняет верификатор с `--require`: **отсутствие/пустота манифеста = fail**, не «OK».

**Freshness — зубы:** закоммитил ещё код после генерации evidence → tip уехал → evidence устарел → CI красный → перегенерь на финальном коммите. Evidence-бандл — ещё и post-hoc audit trail фаундера.

**Нативный фон = обязательный `device_survival` (D3-native, гибрид grill 2026-07-07):** фаза, чей diff совпал с `native_background_permissions`, ОБЯЗАНА объявить `device_survival` и приложить реальный OEM-прогон (≥2 устройства, Xiaomi обяз.). Нет device-evidence → фаза МОЖЕТ закрыться `pass-with-followups` и runner идёт дальше, но **merge PR в main блокирован** до device-evidence ИЛИ явного founder-ack на gap (RUN-QUEUE-запись). Mock-зелёное не закрывает нативный риск (ADR-010).

### Прогнать верификатор локально

```sh
python scripts/autonomy/verify_evidence.py                       # discovery: все specs/*/evidence/*/manifest.json
python scripts/autonomy/verify_evidence.py --phase WAVE0-S2      # одна фаза
python scripts/autonomy/verify_evidence.py --phase MVP0-F2 --require   # native/AI: нет манифеста = FAIL
python scripts/autonomy/verify_evidence.py --head-sha <sha>
```

## Founder one-time actions (ДЛЯ АКТИВАЦИИ auto-merge — сделать позже)

Пока **не сделано** (rails-first). Когда Wave-0 зелёный + CI на secrets, фаундер выполняет:

- ⬜ **Branch protection** на `main` — require PR + required checks (`ci-android`/`ci-backend`/`ci-security`/`ci-evidence`) + linear history + `enforce_admins=true` + `delete_branch_on_merge=true`.
- ⬜ **Вооружить pre-merge хук** — мёржни `settings.hook-snippet.json` в `.claude/settings.json` (Claude не имеет права self-install hook-конфиг).
- ⬜ **Telegram phone-ack** (опц.) — создай `.claude/autonomy/notify.json` с `telegram_chat_id`.
- ⬜ **Флип auto-merge** — сними rails-first-пауза-на-каждом-PR (обнови BUILD-PLAN §Активация + этот README-баннер).

До этого: `/autonomy:run` работает и полезен — он готовит фазы, гоняет гейты, эмитит evidence и **останавливается на PR-гейте** для твоего ack (никакого разрыва с сегодняшним воркфлоу).
