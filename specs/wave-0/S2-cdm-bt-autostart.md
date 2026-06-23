---
spike: WAVE0-S2
title: Автозапуск по подключению наушников (CompanionDeviceManager)
status: approved
blocking: true
owner_role: native-spike-debugger
adr_refs: [ADR-002]
prd_refs: ["§8 Wave 0 S2", "§10.1"]
---

# S2 — CDM-автозапуск захвата по BT из фона

## Гипотеза
При подключении сопряжённых наушников по BT приложение легально стартует сервис захвата **из фона без открытия UI** через `CompanionDeviceManager` + `ACTION_ACL_CONNECTED`. Это ключевой UX «надел наушники → EARAI взвёлся». Риск: ограничения фонового старта FGS, OEM-различия.

## Acceptance criteria (EARS)
- **S2-AC1** — WHEN сопряжённые наушники подключаются по BT, THE SYSTEM SHALL легально стартовать сервис захвата из фона без открытия UI.
- **S2-AC2** — WHERE выдано `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND`, THE SYSTEM SHALL поднять mic-FGS в ответ на `ACTION_ACL_CONNECTED` без нарушения политик фонового старта.
- **S2-AC3** — IF наушники отключаются, THEN THE SYSTEM SHALL корректно останавливать взведённый сервис (не слушать без наушников).

## Метод
1. CDM-ассоциация устройства (one-time онбординг сопряжения).
2. BroadcastReceiver на `ACTION_ACL_CONNECTED` → старт non-mic FGS (взвод) → по триггеру mic-FGS.
3. Прогон: подключить/отключить наушники при закрытом приложении; проверить старт/стоп из логов.

## Device-матрица
| Устройство | OEM | Фокус | Результат |
|---|---|---|---|
| Pixel 8 | Google | базовый CDM-фон-старт | — |
| Redmi/POCO | Xiaomi | автозапуск под MIUI-ограничениями | — |
| Galaxy | Samsung | BT-стек + фон | — |

## Go / No-Go
- **GO:** AC1+AC2 зелёные на ≥2 OEM.
- **Fallback (если CDM капризит):** BT-connect → non-mic FGS с уведомлением «тап, чтобы начать» (тап = легальный mic-старт) + tile. Документировать в `memory/cdm-bt.md`.

## Что решает исход
Определяет primary-путь активации F1. Fallback-цепочка — из S3.

## Эскалация
OEM-различия фон-старта → native-spike-debugger; держать tile (S3) как гарантированный нижний уровень.
