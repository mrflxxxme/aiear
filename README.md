# 🎧 AIEAR — «второй мозг» в любых наушниках

> Android-приложение, превращающее любые Bluetooth-наушники в голосового **ловца мыслей**: поймал идею голосом → ИИ распознал и структурировал → **зафиксировал в действие** (Obsidian / календарь / напоминание / Telegram), не доставая телефон. Дальше — захват встреч/лекций и hands-free ИИ-ассистент.

**Платформа:** Android-first (РФ → рус-Казахстан) · **Сборка:** соло-фаундер + команда профильных ИИ-агентов (spec-driven) · **Статус:** харнесс готов, следующая — блокирующая Wave 0.

Этот репозиторий — не код приложения (он появится в MVP-0), а **планировочная структура и операционная обвязка**, которой ИИ-агенты строят продукт по [PRD](docs/EARAI-PRD.md).

---

<!-- STATUS:BEGIN (авто-обновляется memory-curator из .planning/STATUS.md; руками не править) -->
## 📍 Текущий статус

**Волна:** Wave 0 (спайки риска, блокирующая) · **Активная фаза:** WAVE0-S1 (PR-гейт; решение фаундера) · **Обновлено:** 2026-06-24

| Волна | Статус | Гейт |
|---|---|---|
| **Wave 0** — спайки S1–S6 | 🟡 в работе (S1 на PR-гейте) | S1–S5 зелёные на ≥2 OEM |
| **MVP-0** — «Поймай мысль» (F1–F7) | ⛔ заблокирован Wave 0 | activation >40%, W4-retention >25% |
| **MVP-1** — встречи + биллинг (M1–M5) | ⏸ стаб | Free→Paid 3–6%, маржа >50% |
| **V1 / V2+** — ассистент / B2B | ⏸ стаб | — |

**S1 на гейте:** `app/`-скелет + mic-FGS-спайк готовы, аудит `pass-with-followups`; код/тесты зелёные **по доказательству** (unit 7/7 незав.), но реальный ≥60-мин device-прогон невозможен в облаке → 2 `evidence_gap`. **Следующее действие фаундера:** ратифицировать gap'ы/пины → прогнать `run-on-device.sh` на ≥2 OEM (Xiaomi обяз.) + green-build на раннере с Google-egress → решение гейта.
<!-- STATUS:END -->

---

## 🧠 Как это построено: соло-фаундер + ИИ-агенты

Разработку ведут **11 профильных ИИ-субагентов** по spec-driven процессу. Фаундер — стратег и ревьюер на гейтах, **не кодер**.

```
Wave (роадмап + go/no-go-гейт)
  └─ Phase (фича: 1 spec + 1 plan + 1 PR + 1 phase-аудит)
       └─ Task (декомпозиция planner'а → профильный субагент)
```

**Пайплайн фазы:** `planner` → `android-engineer` ∥ `backend-engineer` → `reviewer` ∥ `reviewer-security` → `verifier` (EARS-тесты + live-gold + evidence) → **phase-аудит** (`architect`, 8 линз) → `memory-curator` → **фаундер аппрувит гейт** → merge.

Пайплайн прогоняет фазу **автономно** и возвращается на гейте; отдельные фазы могут идти независимыми автономными сессиями. Автономность **подтверждается результатами** (ADR-010): фаза доходит до PR только с собранным evidence-бандлом — self-run тестов + **live-gold** на реальных сервисах/устройствах. Подробно — [`.planning/agent-handbook/`](.planning/agent-handbook/00-START-HERE.md).

### Дизайн харнесса (11 зафиксированных решений)

| # | Решение | Выбор |
|---|---|---|
| 1 | Рантайм | Гибрид: native Claude Code субагенты (эфемерные) + AgentDB (память) + cost-телеметрия |
| 2 | Ростер | 7 core + 4 on-demand (Android-перецеленный) |
| 3 | Модели | Opus-дефолт + Sonnet-fallback; pinned-Opus: security/verifier/architect/planner |
| 4 | Хендофы | Лёгкие MD+YAML (не CloudEvents-36-defs) |
| 5 | Память | Общая тегированная `memory/<domain>` + AgentDB-рекалл, single-writer |
| 6 | Пост-аудит | Phase-close обязателен (8 линз) + adversarial на wave-гейтах |
| 7 | Фазировка | 3 уровня Wave→Phase→Task; Wave-0 spike-шаблон; ADR-001..010 апфронт |
| 8 | Контроль | Автономно до фаза-гейта; фаундер ревьюит гейты (тиры 1-5) |
| 9 | Скоуп bootstrap | Харнесс + Wave 0 + MVP-0 детально; поздние волны — стабы |
| 10 | Доставка / язык | PR на ревью; русский + англ. тех-термины; README самообновляемый |
| 11 | Автономность | **Evidence-backed** (ADR-010): self-run тестов + **live-gold** на реальных сервисах/устройствах до PR; «зелёное по утверждению» запрещено |

**Чем эффективнее ORIION:** агенты — нативные **одно-файловые** (одно чтение на спавн), а не 5–7-файловые папки; эфемерный спавн вместо персистентных Opus; лёгкие хендофы; жёсткая контекст-дисциплина + cost-caps. См. [ADR-006](.planning/decisions/ADR-006-agentic-operating-model.md).

### Ростер агентов

| Слой | Роли |
|---|---|
| Оркестрация | `planner` |
| Имплементация | `android-engineer` · `backend-engineer` · `designer`(on-demand) |
| Гейты качества | `reviewer` · `reviewer-security` · `verifier` · `evaluator`(on-demand) |
| Память/арх | `memory-curator` · `architect`(on-demand) |
| Спецназ | `native-spike-debugger`(on-demand) |

Определения — [`.claude/agents/`](.claude/agents/). Модель-роутинг — [`.claude/agents/_shared/model-routing.md`](.claude/agents/_shared/model-routing.md).

---

## 🗺️ Карта репозитория

```
CLAUDE.md                  # канон-контекст агента (читается первым)
README.md                  # этот файл (front page, авто-статус)
docs/                      # PRD + ресёрч (источник продуктовой истины)
.planning/
  PROJECT.md STATUS.md JOURNAL.md OPEN-QUESTIONS.md
  _meta/                   # conventions · glossary · stack
  agent-handbook/          # 00..07 — как работает харнесс (07 = verification & evidence)
  decisions/               # ADR-001..010
  roadmap/                 # волны + гейты
  memory/                  # durable уроки (android-oem, stt-llm, cdm-bt, billing)
.claude/agents/            # 11 native-субагентов + _shared (cost-budget, pipelines)
specs/
  _templates/              # feature-spec · spike-spec
  wave-0/                  # S1–S6 (детально)
  mvp-0/                   # F1–F7 (детально)
  mvp-1/ v1/ v2/           # стабы (JIT)
.github/workflows/         # CI (Android · backend · security)
app/ backend/              # код — появится в MVP-0 (после Wave 0)
```

---

## ▶️ Как запустить фазу (runbook фаундера)

1. **Выбери фазу** в [`.planning/roadmap/`](.planning/roadmap/README.md) (сейчас — `WAVE0-S1`).
2. **Проверь spec** в `specs/<wave>/<PHASE>.md` — он аппрувнут (tier 3+)?
3. **Диспетч:** дай `planner` команду «прогони фазу `<PHASE>`» (автономная сессия). Он выберет pipeline-шаблон, декомпозирует, запустит субагентов.
4. **Гейт:** на выходе получишь PR + сводку + `AUDIT-<PHASE>.md`. Ревью → `merge / revise X / abort`.
5. **Wave-гейт:** на стыке волн — adversarial-аудит + физ-OEM sanity → твой go/no-go.

Разовый ops-setup (то, что агент не сделает): RuStore dev-аккаунт (ИП), keystore/подпись, ключи Yandex Cloud / GigaChat, billing-sandbox, 2–3 физ-OEM. См. [`agent-handbook/06-PR-WORKFLOW.md`](.planning/agent-handbook/06-PR-WORKFLOW.md).

---

## 📚 Ключевые документы

- **Продукт:** [PRD v2.1](docs/EARAI-PRD.md) · ресёрч в [`docs/research/`](docs/research/)
- **Решения:** [ADR-001..010](.planning/decisions/)
- **Верификация/evidence:** [07-VERIFICATION-EVIDENCE](.planning/agent-handbook/07-VERIFICATION-EVIDENCE.md) · [ADR-010](.planning/decisions/ADR-010-evidence-backed-autonomy.md)
- **Операционная модель:** [agent-handbook](.planning/agent-handbook/00-START-HERE.md) · [ADR-006](.planning/decisions/ADR-006-agentic-operating-model.md)
- **Открытые вопросы:** [OPEN-QUESTIONS](.planning/OPEN-QUESTIONS.md)

---

## 🔄 Поддержание README

Секция «Текущий статус» между маркерами `STATUS:BEGIN/END` **регенерируется автоматически** `memory-curator`'ом из [`.planning/STATUS.md`](.planning/STATUS.md) на каждом phase-close; CI-чек валит сборку при рассинхроне. Остальное правится через PR.

---

*AIEAR · spec-driven агентная сборка · РФ-only · конфиденциально*
