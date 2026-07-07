---
id: ADR-011
title: Автономный многофазный runner + gate-authority merge (адаптация ORIION ADR-037)
status: accepted
date: 2026-07-03
supersedes: []
revises: [handbook/06-PR-WORKFLOW]
informs: [ALL]
deciders: [founder]
---

# ADR-011 — Автономный многофазный runner + gate-authority merge

## Контекст

Текущий воркфлоу AIEAR (ADR-006 agentic operating model + ADR-010 evidence-backed автономность + handbook 02-PIPELINE/06-PR-WORKFLOW) исполняется **вручную, одной Claude-сессией на фазу**: фаундер делает preflight, отвечает на форки, говорит «приступай», агенты имплементят + гоняют self-run/live-gold, phase-аудит, PR — и **фаундер читает диф и мёржит каждый tier 3+ PR** (по handbook 06 фаундер — единственный аппрувер tier 3+). Роскошный 11-ролевой pipeline в основном «спроектирован, не исполняется как непрерывная цепь».

Родственный проекту харнесс **ORIION** (`mrflxxxme/oriion`) — из которого AIEAR уже унаследовал каркас (11 ролей, agent-handbook, cost-budget «ORIION-дефолты») — эволюционировал в **автономный runner** (ORIION ADR-037, 8 решений D1–D8, пилот 01.5 пройден). Фаундер просит перенести эту методологию в AIEAR.

Три силы давят на смену: **скорость** (налог ре-bootstrap контекста на каждой фазе), **время фаундера** (в петле ≥4 раза за фазу), **автономность решений** (агенты сами находят *оптимальные* решения, а не собирают мнение по каждому форку).

Constraint, который нельзя нарушить: **«не потерять стабильность».** Для AIEAR это острее, чем для ORIION: главный риск — **нативный фон/OEM** (mic-FGS, CDM, Doze, OEM-киллеры) и качество **ru-STT/LLM**, — его нельзя закрыть mock-зелёным (ADR-010). Доверие переносится с глаз фаундера на машину только при условии, что гейты станут *строже*.

Guiding principle: **автоматизировать полностью, но держать узкие, чётко определённые «растяжки» на том, что может навредить, и делать целостность машины верифицируемой.**

## Decision

Принимаем **автономный многофазный runner, где строгий гейт-стек — merge-authority, а не глаза фаундера**; фаундер в петле только на исключениях. Восемь нормативных решений (адаптация ORIION D1–D8):

### D1 — Review posture: полная автономия мёржа, но **rails-first** (поэтапно)

Целевое состояние: фаундер выходит из merge-петли на всех тирах (ревизует handbook 06 «фаундер — единственный аппрувер tier 3+»). **Но включение обусловлено рельсами D2–D3, которые должны быть построены и зелены ДО передачи merge-authority.**

**Решение интеграции (rails-first):** сейчас строим весь стек, но **auto-merge ВЫКЛЮЧЕН**. Runner доводит фазу до зелёного и **ПАУЗИТ на фаундер-ack на каждом PR** (как автономные сессии сегодня, handbook 06 §Автономные сессии), пока не выполнены оба условия активации:

1. **Wave 0 спайки S1–S5 зелёные** на ≥2 OEM (главный нативный риск снят реальными доказательствами);
2. **CI подключён к secrets** (ci-android FTL, ci-backend live, ci-security) — гейты реально гоняют то, что заявляют.

Тогда фаундер флипает `auto_merge: on` (документируется в `.claude/autonomy/BUILD-PLAN.md` + branch protection). До этого момента D2 (растяжка) всё равно активна как **вторая линия** — даже когда auto-merge включат, растяжка паузит опасное.

### D2 — Задняя растяжка: узкий список категорий → 1-клик ack

Авто-мёрж всё зелёное, **КРОМЕ** изменений, задевающих (совпадает с tier 4, conventions §5 / handbook 06):

- миграции БД по существующим таблицам / RLS,
- auth / аккаунт / сессии / entitlement,
- billing / денежные пути (RuStore Pay / CloudPayments / YooKassa / квоты / 54-ФЗ),
- секреты / ключи / STT-LLM-прокси (Yandex SpeechKit / GigaChat / RuStore, keystore, `.env`),
- ПД / аудио-резидентность (ФЗ-242, RF-облако),
- **нативный фон / разрешения** (mic-FGS / CDM / MediaSession / manifest / tile) — **AIEAR-специфичная категория, главный риск проекта**,
- слом публичных API-контрактов app↔backend.

Эти фазы готовятся, доводятся до зелёного и **ПАУЗЯТ на фаундер 1-клик ack** перед мёржем. Детект — по path-глобам диффа (`.claude/autonomy/tripwire.yaml`, классификатор `scripts/autonomy/classify_tripwire.py`). Позиция v1: любое совпадение = ack (без контентной проверки миграций, пока нет backend).

### D3 — Целостность гейтов: evidence-артефакт + CI-verify + **device-evidence**

Гейты, которые GitHub CI прогнать **не может** (live-gold ru-STT/LLM с funded ключами, RuStore Pay sandbox, **on-device OEM survival** через FTL/agent-device/физ-OEM, adversarial audit), обязаны эмитить **коммит-привязанный evidence-артефакт**: `{gate, head_sha, timestamp, verdict, cost_usd, details}` (schema `.claude/autonomy/evidence-schema.json`). CI-job `ci-evidence` ассертит: для заявленных фазой в `evidence/manifest.json` гейтов артефакт существует, свежий (`head_sha == PR head`), `verdict == PASS`. Freshness — зубы: закоммитил код после генерации evidence → tip уехал → evidence устарел → CI красный → перегенерь на финальном коммите.

**Прямое продолжение ADR-010** (evidence-backed автономность): D3 делает live-gold/device-evidence *неподделываемыми* и *привязанными к коммиту* — то, чего ADR-010 требовал по духу, теперь machine-enforced.

### D3-native — device-evidence обязателен для нативного фона (решение фаундера)

Нативный фон — И tripwire-категория (D2), И **обязательный device-gate**: фаза, чей diff совпал с `native_background_permissions`, ОБЯЗАНА объявить гейт `device_survival` в `evidence/manifest.json` и приложить device-evidence (реальный OEM-прогон, ≥2 устройства, Xiaomi обязателен — memory/android-oem). Нет device-evidence → `evidence_gap` → RUN-QUEUE `stuck`, не тихий mock-зелёный скип. `requires_device_evidence: true` у категории в `tripwire.yaml` — источник истины этого требования.

> Семантика «нет device-evidence → stuck» и путь `evidence/` ревизованы Поправкой 2026-07-07 (см. §Поправка ниже): гибрид pass-with-followups + merge-блок; манифест — внутри `specs/<wave>/evidence/<PHASE>/`.

### D4 — Передняя растяжка: эскалация только на product/market + необратимом

Агент **владеет всеми implementation + архитектурными форками** (пишет ADR через template + логирует в `DECISIONS-LOG.md` через `log_decision.py`). Эскалирует к фаундеру **только**: (1) продуктово-рыночное поведение, требующее знания ЦА/рынка (что видит юзер, цена/тариф, scope-cut, позиционирование), (2) трипвайр-категории D2. Передняя растяжка — зеркало задней. Контракт — `.claude/autonomy/escalation-policy.md`.

### D5 — Оптимальность: judge-панель только на широких форках

Большинство задач — один проход. На **широких форках** (архитектура / алгоритм / схема, высокий blast-radius) — N независимых подходов → роль `evaluator` ранжирует по рубрике **корректность → безопасность/целостность (вкл. ФЗ-152/242) → простота → стоимость → перформанс** → победитель + прививка лучших идей. `planner` флагует широкие форки в `PLAN.md` (`wide_fork: true`). Контракт — `.claude/autonomy/judge-panel.md`.

### D6 — Runner: секвенциальная цепь в одной сессии + opt-in worktree-параллелизм

Фаундер запускает `/autonomy:run`. Runner крутит цикл по очереди фаз:
`discuss(auto) → plan → execute (+ judge-панель на широких форках) → гейты (CI + evidence + device) → tripwire? ack : (auto-merge когда включён | пауза-на-ack rails-first) → следующая фаза`,
пока не упрётся в: эскалацию (D4) / ack (D2) / красный гейт, который fix-loop не чинит (D7) / пустую очередь / budget hard-cap. Независимые треки — opt-in worktree (max 2, мёржи всегда секвенциальны). Убирает налог ре-bootstrap. Команда — `.claude/commands/autonomy/run.md`.

### D7 — Rollback: авто-реверт регрессии + автономный fix-loop + уведомление

Если авто-смёрженная фаза позже роняет гейт на `main`, runner **автоматически ревертит** тот мёрж (`main` зелёный по построению), спавнит **автономный fix-цикл** (max 3), и **уведомляет фаундера о реверте немедленно**. Команда — `.claude/commands/autonomy/heal.md`, детект — `scripts/autonomy/check_main_health.py` (gate-workflows: ci-android/ci-backend/ci-security/ci-evidence). Под rails-first (auto-merge off) D7 срабатывает реже, но остаётся вооружён для пост-мёрж регрессий.

### D8 — Notify: desktop push + RUN-QUEUE + Telegram-мост

Runner обязан достучаться до фаундера на **5 interrupt-событиях**: нужен ack / product-эскалация / случился реверт / красный гейт застрял / прогон завершён. Каналы: `PushNotification` (десктоп) + `.planning/_session-context/RUN-QUEUE.md` (единое окно) + опционально **Telegram-мост** (`.claude/autonomy/notify.json` с `telegram_chat_id`, создаёт фаундер).

### Порядок сборки (предохранители ДО автономии)

- **Блок A — Рельсы:** evidence-схема + `ci-evidence.yml` + tripwire-конфиг + classify + pre-merge хук. ✅ (эта интеграция)
- **Блок B — Фронт:** escalation-policy + decisions-log + judge-панель. ✅
- **Блок C — Runner:** `/autonomy:run|ack|discuss` + RUN-QUEUE + notify + role-loader. ✅
- **Блок D — Self-healing:** `/autonomy:heal` + check_main_health. ✅
- **Блок E — Параллелизм:** opt-in worktree. ✅ (документирован в run.md)
- **Активация auto-merge (D1):** ⏸ **отложена до Wave-0-green + CI-secrets** (фаундер флипает). Branch protection + hook-arming — founder one-time actions (см. `.claude/autonomy/README.md`). *(+3-е условие — evidence-контур, см. §Поправка 2026-07-07.)*

## Поправка 2026-07-07 (grill по проектной документации)

Ратифицировано фаундером в интервью — протокол [`GRILL-2026-07-07-project-docs.md`](../_session-context/GRILL-2026-07-07-project-docs.md) (раунд 4 + производные решения A5–A8). Историю решения выше не переписываем; действующая семантика — эта:

1. **Единый путь evidence (A5).** Машинные `manifest.json` + `<gate>.json` живут **внутри** человеческого бандла ADR-010: `specs/<wave>/evidence/<PHASE>/manifest.json` + `specs/<wave>/evidence/<PHASE>/<gate>.json`. Корневой `evidence/` упразднён (две системы evidence сведены в одну). `verify_evidence.py` в discovery-режиме обходит `specs/*/evidence/*/manifest.json`; манифест обязан перечислять **все** гейты DoD фазы.
2. **D3-native — гибрид вместо `stuck` (решение 4.1).** Нативная фаза без device-evidence МОЖЕТ закрыться `pass-with-followups`; runner продолжает следующую фазу; **merge PR в `main` блокирован** до device-evidence ИЛИ явного founder-ack на gap (RUN-QUEUE-запись). Согласовано с ADR-010 §Решение п.5 (та же поправка там). Двойной гейт (растяжка + device-evidence) сохраняется — гибрид меняет точку блокировки: не конвейер, а мёрж.
3. **3-е условие активации auto-merge (решение 4.4).** К «Wave-0-green» и «CI-secrets» добавлено: **«evidence-контур реально работает»** — единый путь evidence, машиночитаемый DoD, непустой `manifest.json` обязателен для нативных/AI-фаз, `verify_evidence.py --require` фейлит при отсутствии/пустоте манифеста.
4. **Смежные производные (для навигации):** очередь фаз — `.planning/roadmap/phase-queue.yaml` (A6); машинный гейт одобрения спеки — `status: approved` во фронтматтере (A7, handbook 02); каноническая конвенция веток — `claude/*` (A8, conventions §7).

## Consequences

- ✅ **Скорость:** цикл фазы теряет ре-bootstrap-налог; фазы сцепляются в одной сессии (даже под rails-first — паузит только на PR-гейте, не на форках).
- ✅ **Верифицируемая целостность:** D3 + D3-native делают live-gold/device-гейты неподделываемыми; `main` зелёный по построению (D7). Прямое исполнение constraint'а «не потерять стабильность» — для AIEAR это критично из-за нативного риска.
- ✅ **Audit trail:** evidence-артефакты + `DECISIONS-LOG.md` + `RUN-QUEUE.md` = полная картина «что случилось, пока меня не было».
- ✅ **Совместимо с текущим харнессом:** rails-first означает, что до активации auto-merge поведение = сегодняшним «автономным сессиям» (handbook 06), т.е. никакого разрыва; надстройка, не замена.
- ⚠️ **Trade-off — стоимость:** автономный прогон + judge-панель жгут токены; runner уважает `cost-budget.yaml` (per-day soft $30 / hard $75, per-month kill $500) и принимает per-run budget-cap.
- ⚠️ **Trade-off — риск авто-реверта:** смягчается обратимостью реверта + немедленным уведомлением (D7/D8). Под rails-first почти не активен.
- ⚠️ **Разрыв реализации:** 11 ролей — однофайловые хендбуки `.claude/agents/<role>.md`, не нативные spawn-абельные сабагенты; закрыт role-loader'ом `scripts/autonomy/load_role.py` (адаптирован под однофайловый формат AIEAR).
- 🔮 **Future:** cron-режим (безнадзорный ночной прогон) отклонён, пока device-гейты и funded-.env под фаундер-контролем.

## Alternatives Considered

| Альтернатива | Contra | Почему отклонили |
|---|---|---|
| **Полная автономия сразу** (D1-alt) | У AIEAR почти нет кода / CI-secrets / пройденных device-гейтов — предохранительная сеть тонка | Против «не потерять стабильность»; rails-first даёт ту же машину без риска раннего merge-authority (выбор фаундера) |
| **Только фундамент — ADR+доки без скриптов** | Методология остаётся «спроектирована, не исполняется» | Не бьёт в налог скорости/времени; фаундер выбрал rails-first с исполняемым стеком |
| **Полное выравнивание под ORIION-структуру** (contracts/gates/risks/re-numbering) | Крупно и разрушительно для того, что у AIEAR уже работает | Выбран «автономный слой поверх» (выбор фаундера) |
| **Нативный фон только device-evidence, без растяжки** | Нативный код мёржится без 1-клика фаундера | Главный риск EARAI — фаундер выбрал И растяжку И device-gate |
| **CI — твёрдый гейт, локальные — self-report** (D3-alt) | live/device держатся на честном слове агента | Дыра целостности под автономией — неприемлемо (усиливает ADR-010) |

## Links

- **Адаптирует:** ORIION [ADR-037](https://github.com/mrflxxxme/oriion/blob/main/.planning/decisions/ADR-037-autonomous-multiphase-runner.md) (8 решений D1–D8, пилот 01.5)
- **Ревизует:** [handbook 06-PR-WORKFLOW](../agent-handbook/06-PR-WORKFLOW.md) tier-таблица «фаундер — единственный аппрувер tier 3+» → D1 (условно, при активации auto-merge)
- **Расширяет:** [ADR-006](./ADR-006-agentic-operating-model.md) (операционная модель → D6 runner); [ADR-010](./ADR-010-evidence-backed-autonomy.md) (evidence-backed автономность → D3 machine-enforced + D3-native device-gate)
- **Использует:** [ADR-002](./ADR-002-activation-path-b.md) (нативные баны → tripwire native-категория); роль `evaluator` (D5); `verifier`/`native-spike-debugger` (device-evidence D3-native)
- Cost: [`.claude/agents/_shared/cost-budget.yaml`](../../.claude/agents/_shared/cost-budget.yaml) — per-run budget-cap
- Handbook: [08-AUTONOMOUS-RUNNER.md](../agent-handbook/08-AUTONOMOUS-RUNNER.md) — операционный гайд
- Конфиг: [`.claude/autonomy/`](../../.claude/autonomy/README.md) — рельсы + растяжки + команды
