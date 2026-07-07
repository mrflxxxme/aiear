---
description: Автономный многофазный runner — discuss→plan→execute→gates→PR→(rails-first ack | auto-merge), цепь до эскалации/ack/stuck/пустой очереди (ADR-011 D6)
argument-hint: [phase-ids... | next N | until <phase>] (default: next 1)
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent, Skill, TaskCreate, TaskUpdate, ToolSearch
---

# /autonomy:run — автономный многофазный runner (ADR-011 D6/D7/D8)

Ты — runner. Усиленный гейт-стек — merge-authority, НЕ глаза фаундера. Ты сцепляешь фазы в ЭТОЙ сессии (без per-phase ре-bootstrap) до прерывания или пустой очереди. Очередь: **$ARGUMENTS** (default: следующая фаза из **`.planning/roadmap/phase-queue.yaml`** — машиночитаемый бэклог, A6: бери первую `status: ready` с закрытыми `deps`; НЕ выбирай фазу по прозе STATUS.md).

> ⚠️ **RAILS-FIRST (текущий режим, ADR-011 D1):** auto-merge ВЫКЛЮЧЕН. Ты доводишь фазу до зелёного и **ПАУЗИШЬ на фаундер-ack на КАЖДОМ PR** (не только на трипвайре). Auto-merge включается фаундером после **трёх** условий: Wave-0-green + CI-secrets + evidence-контур реально работает (grill 2026-07-07, решение 4.4; см. `.claude/autonomy/BUILD-PLAN.md` §Активация). Пока — шаг Merge всегда идёт по ack-пути.

## Контракты (грузи JIT, в этом порядке)
1. `.planning/agent-handbook/00-START-HERE.md` — bootstrap-маршрут + правила.
2. `.claude/autonomy/escalation-policy.md` (D4) + `judge-panel.md` (D5) + `tripwire.yaml` (D2).
3. `.claude/agents/_shared/cost-budget.yaml` — dev_team caps; уважай per-day soft/hard.
4. `.planning/agent-handbook/07-VERIFICATION-EVIDENCE.md` — что есть evidence/live-gold/device.

## Preflight (раз за прогон)
- `git rev-parse --show-toplevel` — якорь; синхронизируй `origin/main`; работай от свежего `main`.
- **Device-гейты (гибрид, grill 2026-07-07 / 4.1):** on-device OEM survival НЕ гоняется в облаке — это founder/FTL (промежуточный smoke-ярус — Docker-эмулятор фаундера, см. handbook 07 §6b). Фаза, чей diff трогает нативный фон (`native_background_permissions`), обязана дать `device_survival`-evidence; если устройства/FTL нет → явный `evidence_gap`, фаза МОЖЕТ закрыться `pass-with-followups`, ты продолжаешь следующую фазу, но **merge PR в main блокирован** до device-evidence ИЛИ явного founder-ack на gap (RUN-QUEUE `ack-needed` с пометкой native-gap).
- **Funded `.env`:** `python scripts/autonomy/provision_env.py` (idempotent, secret-safe; exit 2 = нет canonical env → live-gold недоступен → stuck-путь для AI-фаз). НИКОГДА не коммить `.env` (трипвайр `secrets_keys_crypto` + gitleaks).
- Бюджет: dev_team per_day soft $30 / hard $75. Трекай приблизительный спенд; СТОП на hard cap (RUN-QUEUE `stuck`: budget).

## Per-phase цикл
Для каждой фазы P в очереди:

0. **Spec-гейт (A7):** прочитай фронтматтер спеки P (путь — из `phase-queue.yaml`). `status: approved` — машинный гейт одобрения фаундером. Поле отсутствует или ≠ `approved` (draft) → **НЕ стартуй фазу**: RUN-QUEUE `escalation` («спека не одобрена») + notify, бери следующую фазу.
1. **Ветка** `claude/auto-<P>-<slug>` от свежего `origin/main`.
2. **Discuss** — гоняй `/autonomy:discuss` для P (own+log через `log_decision.py`; широкие форки → judge-панель; продукт/трипвайр → эскалация). Эскалация: RUN-QUEUE `escalation` + notify; блокирует всю фазу → скипай P (оставь ветку), бери следующую НЕЗАВИСИМУЮ; иначе продолжай незаблокированную часть.
3. **Plan** — `PLAN-<P>.md` ролью planner; пинь таски через TaskCreate.
4. **Execute** — атомарные коммиты (conventional, поле `Pipeline-role:`). Делегируй ролям через Agent: компонуй промпт `python scripts/autonomy/load_role.py --role <role> [--with-routing]` + контекст таска. Ревью — reviewer ∥ reviewer-security (параллельно где независимо). Нативное — `android-engineer`, эскалация багов фона → `native-spike-debugger`. Stagnation kill-switch: нет коммита/файл-райта/статус-апдейта 30 мин wall-clock → abort таск, RUN-QUEUE `stuck` + notify (cost-budget operational).
5. **Gates** — локальный CI-эквивалент:
   - Android: `./gradlew :app:assembleDebug :app:testDebugUnitTest ktlintCheck detekt` (+ `assembleDebugAndroidTest` если менялись instrumented).
   - Backend (когда есть): `ruff check . && mypy --strict . && pytest`.
   - Local-only гейты (live-gold STT/LLM / RuStore sandbox / **device_survival** / adversarial audit / judge-панель) ОБЯЗАНЫ писать `specs/<wave>/evidence/<P>/<gate>.json` (schema `.claude/autonomy/evidence-schema.json`, `head_sha` = финальный коммит) + объявить ВСЕ гейты DoD в `specs/<wave>/evidence/<P>/manifest.json` — единый путь evidence (grill 2026-07-07 / A5), внутри человеческого бандла ADR-010. Самопроверка: `python scripts/autonomy/verify_evidence.py --phase <P>`; для native/AI-фаз — с `--require` (нет/пустой манифест = fail). **Перегенерь evidence-гейты, если коммитил после генерации** (freshness enforced by ci-evidence).
6. **Exit ritual** — JOURNAL append + хендоф (house-rule; review-гейт блокирует мёрж без него). STATUS/JOURNAL/memory + **`status` фазы в `.planning/roadmap/phase-queue.yaml`** пишет ТОЛЬКО через `memory-curator` (handbook правило) — спавни его на exit ritual.
7. **PR** — `gh pr create` (тело по шаблону handbook 06: что/EARS-AC/аудит/девайс/evidence/cost/чек фаундера). Смотри `gh pr checks <N> --watch`; ВСЕ чеки зелёные — включая path-фильтрованные `ci-android`/`ci-backend`, когда сработали. Красный гейт → фикси и re-push, max 3 цикла → RUN-QUEUE `stuck` + notify, дальше.
   > В окружении без `gh` CLI — используй GitHub MCP-тулы (`create_pull_request`, `pull_request_read`, `get_check_run`, `merge_pull_request`) как эквивалент.
8. **Tripwire classify (явный шаг)** — `python scripts/autonomy/classify_tripwire.py --diff-base origin/main` (exit 0 = clean; 10 = matched). Pre-merge хук перепроверяет это на merge-команде — defense-in-depth.
9. **Merge** —
   - **RAILS-FIRST (сейчас):** независимо от трипвайра → RUN-QUEUE `ack-needed` (`run_queue.py add --kind ack-needed --pr <N> --phase <P> --summary ... --details "..."`) + notify. НЕ мёржи. Продолжай следующую фазу ТОЛЬКО если независима от P; иначе стоп (оставь всё зелёным + документированным).
   - **После активации auto-merge (фаундер флипнул):** exit 0 + `device_evidence_required_by` пуст или закрыт → `gh pr merge <N> --squash --delete-branch`; exit 10 (или требуется device-evidence) → RUN-QUEUE `ack-needed` + notify.
10. **Post-merge regression watch (D7)** — только когда мёржи реально происходят (после активации): `python scripts/autonomy/check_main_health.py` (a) ПЕРЕД каждым следующим мёржем и (b) в конце прогона. Exit 20 → **`/autonomy:heal`**; exit 1 → стоп мёржей, `stuck` + notify.
11. **Phase complete** — RUN-QUEUE `complete` (PR, cost, decisions count). Следующая фаза.

## Interrupts → notify (D8), каждый раз
На 5 событиях — **ack-needed / escalation / revert / stuck / run-complete**:
1. `run_queue.py add ...` (очередь = единое окно фаундера).
2. `ToolSearch "select:PushNotification"` → PushNotification (короткий title + что ждёт).
3. Если `.claude/autonomy/notify.json` есть и содержит `telegram_chat_id` → тот же текст в Telegram (phone-ack). Нет/фейл → desktop push + очередь достаточно; никогда не блокируйся на фейле нотификации.

`/autonomy:ack <ID> approved` (фаундер, из любой сессии) разблокирует соответствующий мёрж.

## Parallel tracks (Блок E — opt-in, D6)
Секвенциально — DEFAULT. Параллель ТОЛЬКО когда фазы **доказуемо независимы**: нет общих контекстов (сравни `app/`/`backend/`-области спеков), нет dependency-edge в роадмапе, ни одна не tripwire-heavy. Тогда:
- Спавни per-phase executor-сабагентов через Agent с `isolation: "worktree"` + `run_in_background: true` (max **2** трека), каждый свою ветку + PR через полный цикл (2–7).
- **Мёржи секвенциальны** через шаги 9–10 в main-сессии (один мёрж → health-check → следующий). Атрибуция регрессии (D7) требует одного offender'а за раз.
- Любое сомнение в независимости — не параллель.

## Budget accounting (cost-budget)
В конце прогона оцени спенд (фазы × avg task cost vs dev_team caps) в `complete`-запись. Judge-панели и heal fix-loop'ы считаются против того же per-day бюджета; деградируй N=3→2 при тесноте.

## Stop conditions
Очередь пуста · эскалация/ack блокирует все оставшиеся · budget hard-cap · heal ушёл `stuck` · фаундер сказал стоп. На стопе: RUN-QUEUE `complete` сводка прогона + notify.

## Hard rules
- НИКОГДА не мёржи без: все CI зелёные + evidence свежий + tripwire exit 0 (или approved ack) + (после активации) main healthy. **Под rails-first — не мёржи вообще, только ack-путь.**
- НИКОГДА не байпась хуки (`--no-verify`), никогда `--force` (только `--force-with-lease` на feature-ветках).
- НИКОГДА не выдумывай значения секретов; никогда не коммить `.env`/keystore; никогда не старти device-гейт как mock-зелёный.
- Нативный фон = растяжка + обязательный device_survival-evidence (ADR-011 D3-native).
- Parallel: max 2, доказуемая независимость, мёржи всегда секвенциальны.
