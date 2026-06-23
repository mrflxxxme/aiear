# memory: cdm-bt

## [seed] 2026-06-23 — CompanionDeviceManager = легальный фон-старт по BT
**Источник:** [tech-scenarios-findings](../../docs/research/tech-scenarios-findings.md).
`CompanionDeviceManager` + `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND` + слушать `ACTION_ACL_CONNECTED` → легально стартовать mic-FGS из фона при подключении наушников. «Надел наушники → EARAI взвёлся».
**Как применять:** S2 валидирует это; F1 строит захват на этом пути (ADR-002).

## [seed] 2026-06-23 — Fallback-цепочка активации
Если CDM-автозапуск не сработал на OEM: BT-connect → non-mic FGS с уведомлением «тап, чтобы начать» (тап = легальный старт mic-FGS). + Quick Settings tile (`startActivityAndCollapse`) как ручной лаунчер (0 батареи, 0 риска). + media-button hook (с дебаунсом).
**Как применять:** S3 проверяет tile + media-button; держать как fallback к CDM.

## [seed] 2026-06-23 — Tile — самый безопасный путь
Quick Settings tile: 0 батареи, 0 риска отклонения, работает на всех OEM. Это надёжный нижний уровень активации, когда CDM капризничает.
