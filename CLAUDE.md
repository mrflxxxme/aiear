# CLAUDE.md — Канонический контекст AIEAR

> Этот файл — первое, что читает любой ИИ-агент в этом репозитории. Он задаёт **что мы строим**, **как мы это строим силами агентов** и **жёсткие правила**. Детали — в [`.planning/agent-handbook/`](.planning/agent-handbook/00-START-HERE.md).

## 1. Что это

**AIEAR** (он же EARAI) — Android-приложение, превращающее любые Bluetooth-наушники в «ловца мыслей» для второго мозга: поймал идею голосом → ИИ распознал и структурировал → **зафиксировал в действие** (Obsidian / календарь / напоминание / Telegram), не доставая телефон. Далее — захват встреч/лекций (биллинг) и hands-free ассистент (V1).

- **Платформа:** Android-first (RuStore + ИП), iOS — волной позже.
- **Гео:** РФ-only → рус-Казахстан fast-follow; `ru`-only.
- **Канонический PRD:** [`docs/EARAI-PRD.md`](docs/EARAI-PRD.md) — единственный источник продуктовой истины. Этот харнесс — исполнительная обвязка вокруг него.

## 2. Как мы строим: операционная модель (соло-фаундер + ИИ-агенты)

Разработку ведут **профильные ИИ-субагенты** по spec-driven процессу. Фаундер — стратег и ревьюер на гейтах, **не кодер**. Полная механика — [`.planning/agent-handbook/`](.planning/agent-handbook/00-START-HERE.md). Кратко:

```
Wave (роадмап + go/no-go-гейт)
  └─ Phase (фича/кластер: 1 spec + 1 plan + 1 PR + 1 phase-аудит)
       └─ Task (декомпозиция planner'а, исполняется субагентом)
```

**Пайплайн фазы:** `planner` → (`designer`?) → `android-engineer` ∥ `backend-engineer` → `reviewer` ∥ `reviewer-security` → `verifier` → **phase-close аудит** (`architect`) → `memory-curator` → **фаундер аппрувит гейт** → PR merge.

### Ростер (11 ролей) и модель-роутинг

| Слой | Роль | Модель | Когда |
|---|---|---|---|
| Оркестрация | `planner` | **Opus (pinned)** | каждая фаза: spec→tasks, выбор пайплайна |
| Имплементация | `android-engineer` | Opus↔Sonnet | Kotlin/Compose/Gradle, BT/CDM/mic-FGS/MediaSession |
| Имплементация | `backend-engineer` | Opus↔Sonnet | FastAPI/Pydantic, STT-LLM-прокси, биллинг, sync |
| Гейт | `reviewer` | Sonnet↔Opus | ревью кода, грузит android\|backend-чеклист + ADR-conformance |
| Гейт | `reviewer-security` | **Opus (pinned)** | секреты/PII/ФЗ-152-242/биллинг |
| Гейт | `verifier` | **Opus (pinned)** | EARS как тесты + девайс-петля (FTL + agent-device) |
| Память | `memory-curator` | Sonnet | STATUS/JOURNAL/memory + индекс AgentDB + архивация |
| On-demand | `architect` | **Opus (pinned)** | ADR / кросс-фазные инварианты / phase-close-аудит |
| On-demand | `evaluator` | Opus | AI-промпт-фазы (F2, M2): golden + adversarial, WER |
| On-demand | `native-spike-debugger` | Opus | эскалация по OEM/нативным багам, agent-device-петля |
| On-demand | `designer` | Sonnet | UI-тяжёлые фазы: Compose-спеки/моки |

Политика моделей и триггеры эскалации/fallback — [`.claude/agents/_shared/model-routing.md`](.claude/agents/_shared/model-routing.md). Бюджеты — [`.claude/agents/_shared/cost-budget.yaml`](.claude/agents/_shared/cost-budget.yaml).

## 3. Жёсткие правила (always enforced)

- **Делай ровно то, что в spec** — ни больше, ни меньше. Неясность → эмить `task.unclear` к `planner`, не додумывай.
- **Никогда не коммить секреты** (ключи Yandex Cloud / GigaChat / RuStore, keystore, `.env`). Секреты — только в CI-secrets/env, вне контекста агента.
- **ПД россиян (вкл. аудио) — только RF-облако** (ФЗ-242). Никаких зарубежных STT/LLM в MVP (ADR-003, ADR-004).
- **Читай файл перед правкой.** Предпочитай правку существующего файла созданию нового.
- **Файл-организация:** код — в `app/` (Android) и `backend/` (FastAPI); планирование — в `.planning/`; спеки — в `specs/`; доки — в `docs/`. Не сори в корень.
- **Запрещённые нативные пути:** `BOOT_COMPLETED` mic-старт; AccessibilityService-автоматизация на Google Play; silent-audio Now Playing. См. ADR-002.
- **Дисциплина контекста (рычаг токенов №1):** грузи только свой срез (см. [`01-CONTEXT-LOADING.md`](.planning/agent-handbook/01-CONTEXT-LOADING.md)). Не читай весь репозиторий «на всякий случай».
- **Всегда пост-аудит:** фаза не закрывается без `AUDIT-REPORT.md` (см. [`04-POST-AUDIT.md`](.planning/agent-handbook/04-POST-AUDIT.md)).

## 4. Стек

Android: **Kotlin + Jetpack Compose + Gradle** (запиненный тулчейн). Backend: **Python 3.12 + FastAPI + Pydantic**, PostgreSQL (+pgvector), Redis. ИИ: **Yandex SpeechKit** (STT) + **GigaChat/YandexGPT** (LLM), хранение в RF-облаке. Платежи: **RuStore Pay SDK + CloudPayments/YooKassa** (СБП, 54-ФЗ). Детали и версии — [`.planning/_meta/stack.md`](.planning/_meta/stack.md).

## 5. Команды (когда код появится)

```bash
# Android
./gradlew :app:assembleDebug :app:testDebugUnitTest ktlintCheck detekt
# Backend
ruff check . && mypy --strict . && pytest
```

ВСЕГДА прогоняй тесты после правок. ВСЕГДА проверяй, что сборка зелёная, до PR.

## 6. Точка входа агента

Новый агент в репо → читай [`.planning/agent-handbook/00-START-HERE.md`](.planning/agent-handbook/00-START-HERE.md), затем свой файл роли в [`.claude/agents/<role>.md`](.claude/agents/), затем активный `specs/<phase>/spec.md`.
