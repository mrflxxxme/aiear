# 08 — АВТОНОМНЫЙ RUNNER (ADR-011)

> Надстройка над пайплайном (02): как фазы сцепляются автономно, где машина решает сама, а где паузит на фаундера. Канон — [ADR-011](../decisions/ADR-011-autonomous-multiphase-runner.md). Конфиг — [`.claude/autonomy/`](../../.claude/autonomy/README.md).

## TL;DR

Раньше: 1 сессия на фазу, фаундер отвечает на форки + мёржит каждый PR. Теперь: `/autonomy:run` сцепляет фазы в одной сессии; агент **владеет** всеми impl+arch-форками (решает+логирует), эскалирует **только** продукт/рынок + трипвайр-категории. Целостность держится на **evidence-артефактах, привязанных к коммиту** (подделать зелёное нельзя).

## ⚠️ Режим сейчас: RAILS-FIRST — auto-merge ВЫКЛЮЧЕН

Стек построен, но merge-authority ещё НЕ передан машине. Runner доводит фазу до зелёного и **ПАУЗИТ на фаундер-ack на КАЖДОМ PR** (= сегодняшние автономные сессии, handbook 06). Включение auto-merge (фаундер) — после **трёх** условий (grill 2026-07-07, решение 4.4): Wave-0 S1–S5 зелёные на ≥2 OEM **и** CI на secrets **и** evidence-контур реально работает (единый путь, машинный DoD, непустой manifest для native/AI-фаз, `verify_evidence.py --require` фейлит при отсутствии). См. [`.claude/autonomy/BUILD-PLAN.md`](../../.claude/autonomy/BUILD-PLAN.md) §Активация.

## Две растяжки

| | Задняя (D2, `tripwire.yaml`) | Передняя (D4, `escalation-policy.md`) |
|---|---|---|
| Когда | перед мёржем (диф) | на discuss/design-шаге (подход) |
| Триггер | path-глоб диффа | классификация форка |
| Категории | миграции · auth/аккаунт · billing · секреты/ключи · ПД/ФЗ-242 · **нативный фон** · публичные контракты | продукт/рынок · те же трипвайр-категории |
| Действие | RUN-QUEUE `ack-needed` + notify + ждать `/ack` | RUN-QUEUE `escalation` + notify + блок только этого форка |

**Всё остальное агент решает сам** и логирует (`log_decision.py`; arch → ещё и ADR). Литмус эскалации: *«senior-инженер без рыночного контекста всё ещё гадал бы?»* → да → эскалируй.

## Нативный фон — двойной гейт (D3-native)

mic-FGS / CDM / MediaSession / manifest / tile: И трипвайр (ack), И **обязательный `device_survival`-evidence** (реальный OEM-прогон ≥2 устройства, Xiaomi обяз.). **Гибрид (grill 2026-07-07, решение 4.1):** нативная фаза без device-evidence МОЖЕТ закрыться `pass-with-followups`; runner продолжает следующую фазу; **merge PR в `main` блокирован** до device-evidence ИЛИ явного founder-ack на gap (RUN-QUEUE-запись). Mock-зелёное не закрывает главный риск EARAI (ADR-010).

## Evidence-протокол (D3, единый путь — grill 2026-07-07 / A5)

Local-only гейты (live-gold STT/LLM, RuStore sandbox, device_survival, adversarial) пишут `specs/<wave>/evidence/<PHASE>/<gate>.json` (`head_sha` = финальный коммит, `verdict: PASS`) + объявляют **все** гейты DoD в `specs/<wave>/evidence/<PHASE>/manifest.json` — внутри человеческого бандла ADR-010, не в корневом `evidence/`. `ci-evidence` ассертит существование + свежесть + PASS (discovery по `specs/*/evidence/*/manifest.json`); для native/AI-фаз верификатор гоняется с `--require` (нет/пустой манифест = fail). **Коммитил после генерации → перегенерь** (freshness enforced).

## Judge-панель (D5) — только широкие форки

Архитектура/схема с высоким blast-radius → N подходов → `evaluator` рубрика (**корректность → безопасность/ФЗ → простота → стоимость → перформанс**) → победитель + прививка. `planner` флагует `wide_fork: true`. Иначе — один проход.

## Команды

| Команда | Что |
|---|---|
| `/autonomy:run [phases]` | Runner: цепь фаз до прерывания/пустой очереди. |
| `/autonomy:discuss <phase>` | Авто-резолв форков фазы (own+log / escalate). |
| `/autonomy:ack [RQ-ID verdict]` | Фаундер: список pending / резолв (1-клик merge). |
| `/autonomy:heal` | Детект красного main → авто-реверт → fix-loop (D7). |

## Скрипты (`scripts/autonomy/`, stdlib-only)

`classify_tripwire.py` · `verify_evidence.py` · `run_queue.py` · `log_decision.py` · `premerge_hook.py` · `load_role.py` · `check_main_health.py` · `provision_env.py`.

## Что это НЕ меняет

- **single-writer памяти:** STATUS/JOURNAL/memory — по-прежнему только `memory-curator`. RUN-QUEUE/DECISIONS-LOG ведут скрипты runner'а.
- **Пайплайн ролей (02):** тот же 11-ролевой поток; runner его оркеструет, спавня роли через `load_role.py` + Agent.
- **Evidence-backed автономность (ADR-010):** усилена, не заменена — D3 делает её machine-enforced.
- **Нативные баны (ADR-002):** BOOT_COMPLETED mic-старт / AccessibilityService / silent-audio — запрет, не форк.
