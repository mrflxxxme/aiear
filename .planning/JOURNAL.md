# JOURNAL — append-only лог решений и событий

> Пишет `memory-curator` (single-writer). Только добавление, без перезаписи. Формат: `## YYYY-MM-DD — <заголовок>` + что/почему/последствия.

## 2026-07-07 — Grill по проектной документации: 16 решений фаундера + A1–A9

**Что:** проведено углублённое интервью фаундера (4 раунда × 4 вопроса) по итогам тройного аудита (спеки · продуктовый слой · методология) — **16 ратифицированных решений** + **9 производных агентских решений A1–A9**. Протокол: [`_session-context/GRILL-2026-07-07-project-docs.md`](_session-context/GRILL-2026-07-07-project-docs.md). Правки разнесены одним PR: созданы **ADR-012 (auth) / ADR-013 (Yandex Cloud) / ADR-014 (minSdk 31 + тиры) / ADR-015 (inbox-first Notes)**, контракты **`specs/_contracts/`** (openapi.yaml + thought.schema.json — A4), очередь фаз **`roadmap/phase-queue.yaml`** (A6), founder-чеклист **`ONBOARDING-SECRETS.md`** (реш. 4.3), стадия **discuss (grill)** канонизирована в handbook 02, машинный spec-гейт `status: approved` (A7), ветки `claude/*` канонизированы (A8).

**Почему:** автономный цикл (ADR-011) не мог исполнять роадмап «точно и автоматически»: две несовместимые системы evidence, противоречие ADR-010↔ADR-011 по native-gap, очередь фаз в прозе, неавтоматизированный spec-гейт, 4 конвенции веток, ручной контур фаундера не сведён.

**Ключевые методологические решения:** (1) **единый путь evidence (A5)** — машинные `manifest.json`+`<gate>.json` внутри `specs/<wave>/evidence/<PHASE>/`; `verify_evidence.py` — discovery + `--require` (нет/пустой манифест = fail для native/AI-фаз); (2) **native-gap гибрид (4.1)** — фаза без device-evidence закрывается `pass-with-followups`, runner продолжает, merge в main блокирован до device-evidence ИЛИ founder-ack (поправки в ADR-010 §Решение п.5 + ADR-011 §Поправка); (3) **3-е условие auto-merge (4.4)** — «evidence-контур реально работает»; (4) Docker-эмулятор фаундера — smoke-ярус device-петли (4.2); (5) провижининг 4 секретов за 1–2 недели (4.3).

**Последствия:** runner берёт «что дальше» из phase-queue.yaml, не из прозы STATUS; ADR-011 D3-native действует в гибридной семантике; tier-1/2 auto-merge явно помечены приостановленными под rails-first; ручной контур фаундера — один чеклист с ⬜-статусами. Q1/Q3/Q9 закрыты; Q2 (тарифы) остаётся open.

## 2026-07-03 — Интеграция автономной методологии ORIION (ADR-011)

**Что:** перенесён автономный многофазный runner из ORIION (ADR-037) в AIEAR как **ADR-011** + слой `.claude/autonomy/` (tripwire / evidence-schema / escalation-policy / judge-panel / README / BUILD-PLAN / hook-snippet) + `scripts/autonomy/` (8 stdlib-скриптов: classify_tripwire / verify_evidence / run_queue / log_decision / premerge_hook / load_role / check_main_health / provision_env) + `/autonomy:{run,discuss,ack,heal}` + `.github/workflows/ci-evidence.yml` + `.planning/_session-context/{RUN-QUEUE,DECISIONS-LOG}` + handbook `08-AUTONOMOUS-RUNNER.md`.

**Почему:** убрать налог ре-bootstrap (сессия-на-фазу) и вывести фаундера из merge-петли, не потеряв стабильность — гейт-стек становится merge-authority, целостность держится на evidence, привязанном к коммиту.

**Решения интеграции (интервью с фаундером):**
1. **Глубина = rails-first:** весь стек построен, но **auto-merge ВЫКЛЮЧЕН**; runner паузит на фаундер-ack на каждом PR до Wave-0 S1–S5 зелёных на ≥2 OEM + CI-secrets. Тогда фаундер флипает toggle (BUILD-PLAN §Активация).
2. **Ширина = автономный слой поверх:** не трогаем рабочий handbook/ADR/спеки; адаптируем tripwire/гейты/роли под Android+backend. НЕ переносим ORIION contracts/gates/risks/re-numbering.
3. **Нативный фон = двойной гейт (D3-native):** `native_background_permissions` — И трипвайр-категория (ack), И обязательный `device_survival`-evidence (реальный OEM ≥2, mock не закрывает риск EARAI).

**Адаптации под стек AIEAR:** classify_tripwire — stdlib мини-YAML (нет PyYAML/uv), v1 «любое совпадение = ack» (без контентной проверки миграций); load_role — под однофайловые роли `.claude/agents/<role>.md`; гейты — `gradlew` + ruff/mypy/pytest; check_main_health — ci-android/backend/security/evidence; provision_env — root `.env` (Yandex/GigaChat/SaluteSpeech/RuStore). Все 8 скриптов self-протестированы (tripwire/queue/log/load_role/evidence/hook).

**Последствия:** ADR-001..011; STATUS харнесс-таблица обновлена; branch-protection + hook-arming + notify.json — founder one-time actions, отложены до активации auto-merge. Совместимо с текущим воркфлоу (rails-first = сегодняшние «автономные сессии», handbook 06).

## 2026-06-23 — Bootstrap харнесса (грилл-сессия)

**Что:** создана начальная планировочная структура AIEAR на основе PRD v2.1 — `.planning/` спина, 11 native-субагентов, ADR-001..009, спеки Wave 0 (S1–S6) и MVP-0 (F1–F7), CI-шаблоны, README.

**Почему:** соло-фаундер строит силами ИИ-агентов; нужен spec-driven харнесс, исполнимый агентами, дешевле эталона ORIION по токенам.

**Зафиксированные решения дизайна харнесса (грилл):**
1. Рантайм — **гибрид**: native Claude Code субагенты (эфемерные) + AgentDB (семантическая память) + cost-телеметрия.
2. Ростер — **7 core + 4 on-demand**, Android-перецеленный.
3. Модели — **Opus-дефолт + Sonnet-fallback**; pinned-Opus: security/verifier/architect/planner.
4. Хендофы — **лёгкие MD+YAML** артефакты, single-writer консолидация.
5. Память — **общая тегированная** `memory/<domain>.md` + AgentDB-рекалл.
6. Пост-аудит — **phase-close обязателен** + adversarial на wave-гейтах.
7. Фазировка — **3 уровня** Wave→Phase→Task; Wave-0 spike-шаблон; ADR-001..009 апфронт.
8. Контроль — **автономно до фаза-гейта** + независимые автономные сессии; фаундер ревьюит гейты.
9. Скоуп bootstrap — харнесс + Wave 0 + MVP-0 детально; поздние волны — стабы.
10. Доставка — PR на ревью; язык — русский + англ. тех-термины; README самообновляемый.

**Улучшение над ORIION:** агенты — нативные одно-файловые `.claude/agents/<role>.md` вместо 5–7-файловых папок (одно чтение на спавн, дешевле).

**Последствия:** Wave 0 — следующая (блокирующая). До старта кода — разовый ops-setup фаундера (см. STATUS «Следующее действие»).

## 2026-06-24 — Регламент: evidence-backed автономность (ADR-010)

**Что:** добавлено 11-е решение операционной модели. Агенты обязаны **сами** прогонять тесты + **live-gold** (golden/приёмочные сценарии против реальных сервисов/устройств — live STT/LLM, RuStore sandbox, FTL OEM-матрица) **до** PR и приносить воспроизводимые доказательства в `specs/<wave>/evidence/<PHASE>/`. «Зелёное по утверждению» запрещено; live невозможен → явный `evidence_gap`/`blocked`, не тихий mock-скип.

**Почему:** фаундер требует полную, подтверждённую результатами автономность внутри фаз. Для EARAI mock-зелёное не закрывает главный риск (нативный фон/OEM, качество ru-STT/LLM).

**Затронуто:** новый [ADR-010] + handbook [07-VERIFICATION-EVIDENCE]; правки CLAUDE §3/§5, conventions (evidence-путь), handoff-template (`evidence`/`evidence_gap` + `needs-live-evidence`), 04-POST-AUDIT (7→**8 линз**, +live-gold/evidence), 06-PR-WORKFLOW (evidence-prereq + секция Evidence + CI-чек), роли android/backend/verifier/evaluator (self-run + live-gold), 3 pipeline-шаблона, spec-шаблоны + specs/README + F2 (mock→live-gold).

**Последствия:** PR не открывается без evidence-бандла. Live-прогоны жгут API/FTL — отдельная статья в cost-budget (Q7). Автономная сессия фазы обязана дойти до полного бандла перед PR.

## 2026-06-24 — WAVE0-S1: первый спайк (mic-FGS screen-off) доведён до PR-гейта

**Что:** прогнан полный пайплайн фазы WAVE0-S1 автономной сессией (grill→plan→execute→verify→audit→exit). Заложен **реальный `app/`-скелет** (Kotlin/Compose/Gradle, version-catalog, wrapper 8.14.3) с mic-FGS-спайком внутри: `MicForegroundService` (FGS type=microphone, `AudioRecord`-захват, heartbeat), инструментальный survival-тест (screen-off+Doze, param 2↔60 мин), founder-run-kit. PR открыт (draft, tier 4, не смержен).

**Почему:** S1 — «гейт взлёта №1»: без зелёного mic-FGS-выживания не стартуют F1/M1.

**Доказательство (ADR-010):** в облачной песочнице **нет** Android-сборки — org-egress отдаёт **403** на Google Maven (`maven.google.com`/`dl.google.com`, все зеркала; authoritative по proxy-status), **нет** `/dev/kvm`/девайса/FTL-кредов. Доказано live: pure heartbeat-seam **7/7** (kotlinc 2.0.21 + JUnit, независимо перепроверено verifier'ом), wrapper/catalog-wiring, ADR-002-conformance, fail-closed device-парсер. Объявлены **2 evidence_gap**: `needs-google-maven-egress` (полный build) + `needs-device` (реальный ≥60-мин прогон). Без фейк-грина.

**Аудит (architect, 8 линз):** rev1 нашёл **4 major** — device-харнесс мог дать **ложный green** (cross-session merge прятал kill; heartbeat по wall-clock ≠ аудио; START_STICKY; скрипт fail-open). Revision-loop в той же сессии: все 4 исправлены (per-session-парсинг + STALL/byte-floor + START_NOT_STICKY + fail-closed) и ре-верифицированы; rev2 = **`pass-with-followups`** (tests+device fail→pass, 0 major). Урок зафиксирован в `memory/android-oem.md` как чек-лист на любой survival-тест.

**Зафиксированные допущения (на ратификацию фаундера):** пины AGP 8.7.3/Kotlin 2.0.21/compileSdk 35/targetSdk 34/`minSdk 34`; набор OEM Pixel8+Xiaomi+Samsung (Q3); ветка `claude/happy-franklin-jwt600` (вместо `wave/wave-0-s1-mic-fgs`).

**Последствия:** S1-GO **не** взят — реальное device-доказательство на фаундере (run-kit на ≥2 OEM, Xiaomi обяз.) + green-build на раннере с Google-egress. Фаза ждёт решения гейта (merge/revise). Новый инфра-вопрос Q9 (Google-Maven-egress/FTL для Android-фаз).
