---
phase: MVP0-F5
title: Локальная база + история + поиск (sync в RF-облако)
wave: mvp-0
status: approved
tier: 4
pipeline: fullstack
deps: [MVP0-F2, MVP0-F3]
adr_refs: [ADR-004, ADR-012, ADR-013, ADR-015]
ui_spec: true
prd_refs: ["§8 MVP-0 F5", "§10.2"]
---

# MVP0-F5 — Локальная база + история + поиск + sync

## User story
Как пользователь, я хочу видеть историю пойманных мыслей с поиском и синхронизацией между сессиями/устройствами, чтобы мой «второй мозг» был доступен и не терялся.

## Scope
- **Локальный стор уже создан в F1 (ADR-015: Room + SQLCipher, статусная модель)** — F5 строится **поверх** него: история-UI, поиск, облачный sync. F5 не создаёт БД заново.
- История с поиском по тексту/тегам/типу (Room FTS поверх F1-стора).
- Sync в RF-облако (backend + PostgreSQL/pgvector для семантического поиска).

## Non-scope
- Создание локальной персистенции (сделано в F1). Кросс-устройство в полном объёме (база под это закладывается через device→account, ADR-012; UX — MVP-1). Длинная история Pro-тарифа — MVP-1.

## Контракт / интерфейсы
- **Sync:** `POST /v1/sync` — delta-протокол канонизирован в [`specs/_contracts/openapi.yaml`](../_contracts/openapi.yaml): `since_cursor` + `upserts[]` + `deletions[]` (tombstone), конфликты — **LWW по `updated_at`**; спека не дублирует схемы.
- **Auth (ADR-012):** `Authorization: Bearer <device-token>`; данные привязаны к device-scope (после привязки аккаунта в MVP-1 — к account-scope, миграция без перекройки: owner_id с F1).
- **Search:** локальный (Room FTS) + серверный (pgvector) для семантики.
- **Модель:** `Thought` — [`specs/_contracts/thought.schema.json`](../_contracts/thought.schema.json); локальные служебные поля sync (`synced_at`, курсор) — вне канонической схемы.
- **Embedding-модель для pgvector:** выбор отложен в backend-конфиг — **документированное допущение (на ратификацию в PR фазы)**: не блокер фазы, семантический поиск деградирует до FTS до выбора модели.

## Acceptance criteria (EARS)
- **MVP0-F5-AC1** — WHEN мысль зафиксирована, THE SYSTEM SHALL сохранить её в локальную историю немедленно (offline-first).
- **MVP0-F5-AC2** — WHEN есть сеть, THE SYSTEM SHALL синхронизировать локальные изменения в RF-облако и подтянуть удалённые.
- **MVP0-F5-AC3** — WHEN пользователь ищет по тексту/тегу/типу, THE SYSTEM SHALL вернуть релевантные заметки.
- **MVP0-F5-AC4** — IF возникает конфликт sync, THEN THE SYSTEM SHALL разрешить его детерминированно (last-write-wins по timestamp) без потери данных.
- **MVP0-F5-AC5** — THE SYSTEM SHALL хранить ПД только в RF-облаке (ADR-004).

## Edge-cases / unhappy-path
- Долгий офлайн → накопление дельты, корректный батч-sync при возврате сети.
- Большая история → пагинация/ленивая загрузка.
- Удаление заметки → tombstone, корректная пропагация в sync.

## Test plan
- Unit: Room DAO, FTS-поиск, conflict-resolution.
- Integration: sync round-trip (мок backend + sandbox), дельта/конфликт.
- Instrumented: офлайн→онлайн переход.

## Data / privacy
Локальная БД шифрована **SQLCipher, ключ — в Android Keystore** (стор F1); серверное хранение — RF-облако (Yandex Cloud ru-central1, ADR-013), шифрование в покое/транзите (ADR-004).

## Model hints
- Room/FTS/sync-логика — T2/T3 (конфликты — T3).
- pgvector-поиск backend — T2/T3.
