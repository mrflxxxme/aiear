---
phase: MVP0-F1
title: Захват мысли (стриминговый STT)
wave: mvp-0
status: approved
tier: 4
pipeline: fullstack
deps: [WAVE0-S1, WAVE0-S2, WAVE0-S3, WAVE0-S5]
adr_refs: [ADR-002, ADR-003, ADR-004, ADR-009]
ui_spec: true
prd_refs: ["§8 MVP-0 F1", "§10.1", "§10.2"]
---

# MVP0-F1 — Захват мысли

## User story
Как Анна/Дмитрий, я хочу поймать мысль голосом через наушники на ходу, чтобы она не потерялась за 20–30 сек «остановиться → достать телефон».

## Scope
- Запуск сессии: tile / BT-autostart (CDM) / кнопка в приложении / media-hook / wake-word (если S6 зелёный, пока наушники подключены).
- Запись короткой реплики → стриминговый STT (Yandex SpeechKit через backend-прокси).
- Показ промежуточного и финального транскрипта.

## Non-scope
- Структурирование/классификация (F2). Фиксация в destination (F3). Офлайн (F6). Голос-команды (F7).

## Контракт / интерфейсы
- **Клиент → backend:** `POST /v1/stt/stream` (WebSocket/gRPC), аудио-чанки; ответ — partial/final транскрипт. Ключи STT — только на backend (ADR-004).
- **Модель:** `CaptureSession {id, started_at, source: enum(tile|cdm|button|media|wakeword), transcript_partial, transcript_final, status}`.

## Acceptance criteria (EARS)
- **MVP0-F1-AC1** — WHEN сессия захвата активна, THE SYSTEM SHALL транскрибировать речь и показать текст в течение **3 сек** после окончания реплики.
- **MVP0-F1-AC2** — WHILE пользователь говорит, THE SYSTEM SHALL показывать промежуточный транскрипт (стриминг, <2 сек partial — S5).
- **MVP0-F1-AC3** — WHEN сессия стартует любым из путей (tile/CDM/button/media/wakeword), THE SYSTEM SHALL начать захват <1 сек (tile/media — S3) и пометить `source`.
- **MVP0-F1-AC4** — IF микрофон занят/недоступен, THEN THE SYSTEM SHALL уведомить пользователя и не падать.
- **MVP0-F1-AC5** — WHILE идёт фоновый захват, THE SYSTEM SHALL держать mic-FGS-уведомление (S1).

## Edge-cases / unhappy-path
- Реплика пустая/тишина → не создавать пустую мысль.
- Обрыв сети в середине → задел под F6 (черновой on-device), сейчас — пометить и не терять аудио.
- Наушники отключились в середине → корректно завершить/сохранить.

## Test plan
- Unit: state-machine сессии, маппинг source.
- Contract: STT-стрим (mock + sandbox SpeechKit), partial/final.
- Instrumented: старт всеми путями на ≥2 OEM; mic-FGS screen-off (опирается на S1).
- Latency: partial <2 сек, final <3 сек (4G).

## Data / privacy
Аудио + транскрипт — ПД, хранение в RF-облаке (ADR-004). Аудио не покидает RF. Согласия — на онбординге (F4).

## Model hints
- mic-FGS/CDM/MediaSession/tile — **T3** (нативное, tier 4).
- UI-состояния сессии — T2 (designer + android-engineer).
- backend STT-прокси — T3 (контракт/ключи).
