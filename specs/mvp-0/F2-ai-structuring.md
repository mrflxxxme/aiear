---
phase: MVP0-F2
title: ИИ-структурирование + классификация
wave: mvp-0
status: approved
tier: 4
pipeline: fullstack
deps: [MVP0-F1, WAVE0-S5]
adr_refs: [ADR-003, ADR-004]
ui_spec: false
prd_refs: ["§8 MVP-0 F2", "§10.2"]
---

# MVP0-F2 — ИИ-структурирование + классификация

## User story
Как пользователь, я хочу, чтобы пойманная реплика автоматически превратилась в структурированную единицу (заголовок, тип, теги), чтобы её можно было сразу зафиксировать в действие.

## Scope
- LLM определяет тип, теги, заголовок, нормализованный текст. **Дефолт — GigaChat-2 Lite через сменный адаптер** за backend-прокси (Q1 закрыт, grill 2026-07-07 §3.2; ADR-013 §4); смена провайдера — конфиг backend, не релиз клиента.
- Предложение destination + CTA на основе типа; время CTA LLM отдаёт как RFC3339 (парсер времени — в промпте, не на клиенте).
- **Бенчмарк GigaChat ↔ YandexGPT** — evidence внутри фазы (не блокер гейта) — задача evaluator.

## Non-scope
- Сама фиксация в destination (F3). Голос-команды (F7).

## Контракт / интерфейсы
- **Клиент → backend:** `POST /v1/structure` — запрос/ответ канонизированы в [`specs/_contracts/openapi.yaml`](../_contracts/openapi.yaml); схема `Thought` — [`specs/_contracts/thought.schema.json`](../_contracts/thought.schema.json) (спека не дублирует поля).
- **Канонический LLM-промпт — [`specs/_contracts/prompts/structure_v1.md`](../_contracts/prompts/structure_v1.md)**; материализация в `backend/app/ai/` обязана совпадать с каноном (CI-сверка). Новая версия промпта = `structure_v2.md` + live-gold-прогон.
- Golden dataset — [`specs/_contracts/golden/`](../_contracts/golden/README.md) (`f2-structuring.jsonl`).

## Acceptance criteria (EARS)
- **MVP0-F2-AC1** — WHEN транскрипт готов, THE SYSTEM SHALL вернуть структурированный объект `{заголовок, тип, теги, нормализованный текст}` и предложить destination + CTA.
- **MVP0-F2-AC2** — WHEN тип определён, THE SYSTEM SHALL выбрать дефолтный destination по типу (напр. `reminder` → Календарь/Напоминания; `idea`/`journal` → inbox; enum — по `thought.schema.json`).
- **MVP0-F2-AC3** — IF LLM недоступен/таймаут, THEN THE SYSTEM SHALL сохранить `Thought` со статусом `transcribed` (сырой транскрипт в inbox, ADR-015) и пометить для повторной обработки.
- **MVP0-F2-AC4** — THE SYSTEM SHALL держать точность классификации типа на golden-наборе **≥ 85%** при **WER исходного STT < 15%** (пороги ратифицированы фаундером, grill 2026-07-07 §3.3; WER учитывается отдельной метрикой; пересмотр — только ADR'ом по данным).

## Edge-cases / unhappy-path
- Двусмысленный транскрипт → консервативный тип `other` + низкий `confidence` (правило промпта structure_v1), не выдумывать задачу/время.
- Мат/шум/смешанный язык → adversarial-набор evaluator.
- Очень длинная реплика → усечение/суммаризация с сохранением сырого текста.

## Test plan
- Contract: `/v1/structure` (unit на mock LLM — только для быстрого цикла разработки).
- **Live-gold (ADR-010):** evaluator гоняет golden + adversarial (`specs/_contracts/golden/f2-structuring.jsonl`) против **реальных** GigaChat/YandexGPT (не моков); метрики classification accuracy (≥85%) + structure fidelity на живой модели; бенчмарк GigaChat vs YandexGPT — evidence фазы (Q1 уже закрыт: дефолт GigaChat-2 Lite). Нет live-ключей → `evidence_gap`, не закрывать на mock.
- Regression: при смене версии промпта — live-gold-прогон обязателен.

## Evidence & live-gold plan (ADR-010)
- **Self-run:** `ruff/mypy/pytest` бэка → `evidence/MVP0-F2/selftest-backend.txt`.
- **Live-gold:** golden+adversarial против live GigaChat и YandexGPT → `evidence/MVP0-F2/live-gold-gigachat.json`, `live-gold-yandexgpt.json` (accuracy / structure-fidelity / бенчмарк / WER на стыке с STT).
- **Gap:** нет ключей LLM → `evidence_gap: needs GigaChat/YandexGPT creds` + `blocked` до выдачи фаундером.

## Data / privacy
Транскрипт → LLM в RF-облаке (ADR-004). Никаких зарубежных LLM (ADR-003).

## Model hints
- LLM-промпт-инжиниринг + бенчмарк — **T3** + evaluator.
- backend-эндпоинт — T2/T3.
