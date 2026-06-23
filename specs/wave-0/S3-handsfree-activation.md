---
spike: WAVE0-S3
title: Активация без рук (Quick Settings tile + media-button hook)
status: approved
blocking: true
owner_role: native-spike-debugger
adr_refs: [ADR-002, ADR-009]
prd_refs: ["§8 Wave 0 S3", "§10.1", "§10.3"]
---

# S3 — Активация tile / media-button <1 сек

## Гипотеза
Пользователь стартует сессию захвата без разблокировки/навигации: тап по Quick Settings tile ИЛИ двойное нажатие media-кнопки (когда EARAI — активная медиа-сессия). Старт <1 сек. Риск: ненадёжность media-кнопок по брендам.

## Acceptance criteria (EARS)
- **S3-AC1** — WHEN пользователь тапает tile, THE SYSTEM SHALL начать сессию захвата **< 1 сек**.
- **S3-AC2** — WHEN пользователь даёт двойное нажатие media-кнопки И EARAI — активная медиа-сессия, THE SYSTEM SHALL начать сессию захвата < 1 сек (с дебаунсом).
- **S3-AC3** — IF media-кнопка не доходит до приложения (бренд/AirPods), THEN THE SYSTEM SHALL оставаться полностью управляемым через tile (graceful degradation).

## Метод
1. `TileService` с `startActivityAndCollapse` → старт сессии.
2. `MediaSession` + обработка `KEYCODE_HEADSETHOOK` с дебаунсом двойного нажатия.
3. Замер латентности тапа→захват; прогон media-hook на разных наушниках (TWS с кнопкой, накладные, AirPods).

## Device-матрица
| Устройство / наушники | Фокус | Результат |
|---|---|---|
| Pixel + TWS с кнопкой | tile + media-hook | — |
| Xiaomi + накладные BT | tile латентность | — |
| любой + AirPods | graceful degradation (только tile) | — |

## Go / No-Go
- **GO:** AC1 (tile) зелёный на ≥2 OEM — это гарантированный путь. AC2 (media) — bonus, не блокирует.
- Tile — обязательный нижний уровень; media-hook едет, если стабилен.

## Что решает исход
Подтверждает hands-free-минимум MVP-0. wake-word (S6) — отдельный, более амбициозный путь.

## Эскалация
Media-кнопки нестабильны по брендам — это известно (`memory/android-oem.md`); не вкладываться сверх дебаунса, опираться на tile + CDM (S2).
