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
- LLM (GigaChat/YandexGPT через backend) определяет тип (задача/идея/напоминание/заметка), теги, заголовок, нормализованный текст.
- Предложение destination + CTA на основе типа.
- **Бенчмарк GigaChat ↔ YandexGPT** (закрывает OPEN-QUESTION Q1) — задача evaluator.

## Non-scope
- Сама фиксация в destination (F3). Голос-команды (F7).

## Контракт / интерфейсы
- **Клиент → backend:** `POST /v1/structure` {transcript} → `Thought {title, type: enum(task|idea|reminder|note), tags[], normalized_text, suggested_destination, suggested_cta}`.
- LLM-промпт — версионируемый артефакт (`backend/app/ai/prompts/structure_vN.txt`), проходит evaluator.

## Acceptance criteria (EARS)
- **MVP0-F2-AC1** — WHEN транскрипт готов, THE SYSTEM SHALL вернуть структурированный объект `{заголовок, тип, теги, нормализованный текст}` и предложить destination + CTA.
- **MVP0-F2-AC2** — WHEN тип определён, THE SYSTEM SHALL выбрать дефолтный destination по типу (напр. `reminder` → Календарь/Напоминания; `note`/`idea` → Obsidian).
- **MVP0-F2-AC3** — IF LLM недоступен/таймаут, THEN THE SYSTEM SHALL сохранить сырой транскрипт как `note` и пометить «не структурировано» для повторной обработки.
- **MVP0-F2-AC4** — THE SYSTEM SHALL держать классификацию типа на golden-наборе с accuracy ≥ целевого (порог задаёт evaluator; WER исходного STT учитывается отдельно).

## Edge-cases / unhappy-path
- Двусмысленный транскрипт → консервативный тип `note`, не выдумывать задачу/время.
- Мат/шум/смешанный язык → adversarial-набор evaluator.
- Очень длинная реплика → усечение/суммаризация с сохранением сырого текста.

## Test plan
- Contract: `/v1/structure` (mock LLM + sandbox).
- **Evaluator:** golden dataset (эталонные транскрипт→Thought) + adversarial; метрики classification accuracy + structure fidelity; бенчмарк GigaChat vs YandexGPT → решение Q1.
- Regression: при смене версии промпта — golden-прогон обязателен.

## Data / privacy
Транскрипт → LLM в RF-облаке (ADR-004). Никаких зарубежных LLM (ADR-003).

## Model hints
- LLM-промпт-инжиниринг + бенчмарк — **T3** + evaluator.
- backend-эндпоинт — T2/T3.
