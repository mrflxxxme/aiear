---
id: ADR-013
title: Архитектура backend — FastAPI-монолит в Yandex Cloud, контракты в specs/_contracts
status: accepted
date: 2026-07-07
supersedes: []
informs: [WAVE0, MVP0, MVP1]
deciders: [founder, architect]
---

# ADR-013 — Backend: Yandex Cloud, FastAPI-монолит, канонические контракты

## Контекст
Backend был объявлен обязательным (ADR-004: ключи STT/LLM только на сервере), но не
спроектирован: не существовало ни `backend/`, ни схемы БД, ни единого API-контракта,
ни выбора хостинга (ADR-004 перечислял «Yandex Cloud / VK Cloud / Selectel» как меню).
Эндпоинты были разбросаны прозой по спекам F1/F2/F5. Автономный runner упёрся бы в это
на первой же fullstack-фазе (S5/F1).

## Решение (хостинг ратифицирован фаундером, grill 2026-07-07 §1.4; композиция — architect)
1. **Хостинг: Yandex Cloud** (регион `ru-central1`). Мотив: ко-локация со SpeechKit —
   минимальная сетевая латентность до STT (гейт S5 p95 <2 с), единый биллинг/контракт
   (рычаг объёмных скидок для маржи MVP-1), managed PostgreSQL + pgvector, Serverless
   Containers. Уточняет ADR-004 (выбор из тройки сделан).
2. **Форма: FastAPI-монолит** (Python 3.12 + Pydantic) в одном контейнере; PostgreSQL
   (+pgvector) + Redis. Микросервисы не заводим до V1 — соло-фаундер + агенты, один
   деплой-юнит дешевле в эксплуатации и ревью.
3. **Канонические контракты живут в `specs/_contracts/`:**
   - `openapi.yaml` — единый версионируемый контракт `/v1/*` (stt/stream, structure,
     sync, auth). Спеки фаз ссылаются на него и не дублируют схемы.
   - `thought.schema.json` — каноническая JSON-схема `Thought`.
   Изменение контракта = tripwire «публичные контракты» (ADR-011 D2).
4. **LLM/STT-провайдеры — сменные адаптеры** за прокси: клиент не знает, какая модель
   внутри. Дефолт LLM — GigaChat-2 Lite (grill §3.2), STT — Yandex SpeechKit.
   Смена провайдера — конфиг backend, не релиз клиента.
5. **STT-стриминг: WebSocket** (`/v1/stt/stream`) как default-протокол; gRPC — только
   если спайк S5 покажет, что WS не держит p95 <2 с (документированное допущение A1).
6. **Деплой/CI:** Docker-образ → Yandex Cloud Serverless Containers; отдельный
   `ci-backend` workflow (ruff + mypy --strict + pytest + build). Миграции — Alembic,
   каждая миграция = tripwire (ADR-011 D2).

## Последствия
- До первой fullstack-фазы (F1) заводится скелет `backend/` (app, tests, Dockerfile,
  alembic) — как S1 завёл скелет `app/`.
- `specs/_contracts/` — единственный источник истины API; рассинхрон спека↔контракт — блокер ревью.
- Секреты (SpeechKit, GigaChat) — только env/CI-secrets (ADR-004), свод — `ONBOARDING-SECRETS.md`.

## Альтернативы (отклонены)
- **VK Cloud / Selectel** — нет ко-локации со SpeechKit; Selectel беднее managed-сервисами.
- **Ключи в клиенте без backend** — нарушает ADR-004/ФЗ-242 и tripwire «секреты».
- **Микросервисы сразу** — эксплуатационная цена без выгоды на этом масштабе.
