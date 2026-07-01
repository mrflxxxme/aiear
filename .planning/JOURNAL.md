# JOURNAL — append-only лог решений и событий

> Пишет `memory-curator` (single-writer). Только добавление, без перезаписи. Формат: `## YYYY-MM-DD — <заголовок>` + что/почему/последствия.

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
