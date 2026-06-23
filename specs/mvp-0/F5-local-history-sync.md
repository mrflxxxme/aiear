---
phase: MVP0-F5
title: Локальная база + история + поиск (sync в RF-облако)
wave: mvp-0
status: approved
tier: 4
pipeline: fullstack
deps: [MVP0-F2, MVP0-F3]
adr_refs: [ADR-004]
ui_spec: true
prd_refs: ["§8 MVP-0 F5", "§10.2"]
---

# MVP0-F5 — Локальная база + история + поиск + sync

## User story
Как пользователь, я хочу видеть историю пойманных мыслей с поиском и синхронизацией между сессиями/устройствами, чтобы мой «второй мозг» был доступен и не терялся.

## Scope
- Локальная БД заметок (Room) + история с поиском.
- Sync в RF-облако (backend + PostgreSQL/pgvector для поиска).
- Поиск по тексту/тегам/типу.

## Non-scope
- Кросс-устройство в полном объёме (база под это закладывается, UX — позже). Длинная история Pro-тарифа — MVP-1.

## Контракт / интерфейсы
- **Sync:** `POST /v1/sync` (delta upload/download), conflict-resolution last-write-wins + timestamp.
- **Search:** локальный (Room FTS) + серверный (pgvector) для семантики.
- **Модель:** `Thought` (из F2) + `synced_at`, `local_id`/`server_id`.

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
Локальная БД шифрована; серверное хранение — RF-облако, шифрование в покое/транзите (ADR-004).

## Model hints
- Room/FTS/sync-логика — T2/T3 (конфликты — T3).
- pgvector-поиск backend — T2/T3.
