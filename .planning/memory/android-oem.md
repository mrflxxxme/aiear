# memory: android-oem

## [seed] 2026-06-23 — OEM-киллеры фона = главный риск (R-OEM)
**Источник:** [tech-scenarios-findings](../../docs/research/tech-scenarios-findings.md), [agentic-build-operating-model](../../docs/research/agentic-build-operating-model.md).
MIUI (Xiaomi), Honor/Huawei, Transsion агрессивно убивают фоновые сервисы. Облачная ферма (Firebase Test Lab) покрывает ~80%, но **не OEM-killers** — нужна физическая проверка на 2–3 реальных телефонах. Держать Don't-Kill-My-App чек-лист по бренду.
**Как применять:** verifier гонит mic-FGS на ≥2 OEM (вкл. Xiaomi/Samsung); фаундер делает физ-sanity на wave-гейте.

## [seed] 2026-06-23 — mic-FGS правила (Android 14/15)
`FOREGROUND_SERVICE_MICROPHONE` обязателен для фонового микрофона. Старт — из foreground или через CDM-исключение. **Запрещено:** `BOOT_COMPLETED` mic-старт (бан с Android 14; Android 15 расширил на dataSync/mediaPlayback/camera/phoneCall).
**Как применять:** android-engineer не стартует mic из BOOT_COMPLETED; reviewer блокит такой путь.

## [seed] 2026-06-23 — Роль default-ассистента молча сбрасывается
`ROLE_ASSISTANT` может сбрасываться при переустановке/обновлении. Детектить на каждом старте и переспрашивать. На Samsung боковая кнопка — отдельная настройка. (Актуально для V1, не MVP.)

## [seed] 2026-06-23 — Кнопки наушников портативно не ловятся
Только media-button hook (`KEYCODE_HEADSETHOOK`, play/pause) и только когда EARAI — активная медиа-сессия. AirPods на Android = только answer/hangup/play-pause. Galaxy Buds/Sony/Jabra кастомные жесты обрабатывает вендорское приложение, до стороннего не доходит.
**Как применять:** media-hook — бонус-триггер in-session, не primary путь (ADR-002).
