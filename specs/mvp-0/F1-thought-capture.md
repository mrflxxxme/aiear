---
phase: MVP0-F1
title: Захват мысли (стриминговый STT)
wave: mvp-0
status: approved
tier: 4
pipeline: fullstack
deps: [WAVE0-S1, WAVE0-S2, WAVE0-S3, WAVE0-S5]
adr_refs: [ADR-002, ADR-003, ADR-004, ADR-009, ADR-012, ADR-014, ADR-015]
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
- **Inbox-first фундамент (ADR-015):** локальный шифрованный стор мыслей (Room + SQLCipher) и статусная модель `captured → transcribed → structured → exported` создаются **в этой фазе**; каждая захваченная мысль персистится в inbox ДО любой сетевой обработки. F5/F6/F3 строятся поверх этого стора.
- Продуктовый `minSdk 31` + тиры возможностей (ADR-014) вводятся в этой фазе (`app/build.gradle.kts`).

## Non-scope
- Структурирование/классификация (F2). Фиксация в destination (F3). Офлайн (F6). Голос-команды (F7). История-UI/поиск/облачный sync поверх стора (F5).

## Контракт / интерфейсы
- **Клиент → backend:** **WebSocket `/v1/stt/stream`** — протокол и схемы сообщений (`SttInit`/`SttPartial`/`SttFinal`/`SttError`) канонизированы в [`specs/_contracts/openapi.yaml`](../_contracts/openapi.yaml) (решение A1: WS default; gRPC — только по результату S5). Ключи STT — только на backend (ADR-004).
- **Auth (ADR-012):** перед первым стримом клиент получает анонимный device-token через `POST /v1/auth/device`; все запросы — `Authorization: Bearer <device-token>` (см. openapi.yaml).
- **Модель:** `CaptureSession {id, started_at, source: enum(tile|cdm|button|media|wakeword), transcript_partial, transcript_final, status}`; персистентная единица — `Thought` по [`specs/_contracts/thought.schema.json`](../_contracts/thought.schema.json) (спека не дублирует схему).

## Acceptance criteria (EARS)
- **MVP0-F1-AC1** — WHEN сессия захвата активна, THE SYSTEM SHALL транскрибировать речь и показать текст в течение **3 сек** после окончания реплики.
- **MVP0-F1-AC2** — WHILE пользователь говорит, THE SYSTEM SHALL показывать промежуточный транскрипт (стриминг, <2 сек partial — S5).
- **MVP0-F1-AC3** — WHEN сессия стартует любым из путей (tile/CDM/button/media/wakeword), THE SYSTEM SHALL начать захват <1 сек (tile/media — S3) и пометить `source`.
- **MVP0-F1-AC4** — IF микрофон занят/недоступен, THEN THE SYSTEM SHALL уведомить пользователя и не падать.
- **MVP0-F1-AC5** — WHERE устройство Full-tier (Android 14+, ADR-014), WHILE идёт фоновый захват (вкл. screen-off), THE SYSTEM SHALL держать mic-FGS-уведомление (S1). Screen-off-захват — **только Full-tier**; на Degraded (Android 12–13) не обещается.
- **MVP0-F1-AC6** — WHEN реплика захвачена, THE SYSTEM SHALL персистировать `Thought` в локальный шифрованный inbox-стор (Room+SQLCipher) со статусом `captured`/`transcribed` ДО и независимо от любой облачной обработки (ADR-015: захват не теряется).
- **MVP0-F1-AC7** — WHERE устройство Degraded-tier (Android 12–13, ADR-014), THE SYSTEM SHALL обеспечивать захват при активном приложении/включённом экране и честно не предлагать screen-off-пути.

## Edge-cases / unhappy-path
- Реплика пустая/тишина → не создавать пустую мысль.
- Обрыв сети в середине → задел под F6 (черновой on-device), сейчас — пометить и не терять аудио.
- Наушники отключились в середине → корректно завершить/сохранить.

## Test plan
- Unit: state-machine сессии, маппинг source; inbox-стор (DAO, статусные переходы captured→transcribed, шифрование SQLCipher — ключ в Android Keystore).
- Contract: STT-стрим (mock + sandbox SpeechKit), partial/final.
- Instrumented: старт всеми путями на ≥2 OEM; mic-FGS screen-off (опирается на S1).
- Latency: partial <2 сек, final <3 сек (4G).

## Data / privacy
Аудио + транскрипт — ПД, хранение в RF-облаке (ADR-004). Аудио не покидает RF. Согласия — на онбординге (F4).

## Model hints
- mic-FGS/CDM/MediaSession/tile — **T3** (нативное, tier 4).
- UI-состояния сессии — T2 (designer + android-engineer).
- backend STT-прокси — T3 (контракт/ключи).
