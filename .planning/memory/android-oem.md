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

## [WAVE0-S1] 2026-06-24 — спайк mic-FGS screen-off: уроки (device-исход ещё pending)
**Источник:** фаза WAVE0-S1 (`specs/wave-0/`), аудит `AUDIT-WAVE0-S1.md`, evidence `evidence/WAVE0-S1/`.

**[infra-блокер] Облачная песочница не собирает Android.** Org-egress отдаёт **403** на Google
Maven (`maven.google.com`/`dl.google.com`) и все зеркала → AGP/AndroidX/Compose/SDK недоступны;
любой Gradle-Android-build падает на резолве зависимостей, даже без эмулятора (нет `/dev/kvm`).
**Как применять:** для Android-фаз нужен один из: (1) allowlist Google Maven на сессию, (2)
пред-прогретый `GRADLE_USER_HOME`+`$ANDROID_HOME` (offline), (3) CI-раннер с Google-egress.
Иначе self-run ограничен pure-Kotlin (Maven Central) — это `evidence_gap: needs-google-maven-egress`.
403-политику НЕ обходить (`/root/.ccr/README.md`).

**[design] mic-FGS, что подтвердилось как правильное в коде (компилируется; device-прогон pending):**
`ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)` **до** `AudioRecord`;
тип в манифесте == тип в рантайме; старт только из foreground-Activity; `exported=false`;
`allowBackup=false` (PCM не утекает в бэкап). Для **спайка выживания** — `START_NOT_STICKY`
(kill терминальный и виден как обрыв лога; авто-рестарт — это уже F1-resilience, другое).

**[meta-урок верификации] Тест выживания легко делает ЛОЖНЫЙ green** (аудит нашёл 4 вектора):
(1) merge heartbeat-логов разных сессий + sort прячет kill (t= рестартует с ~0) → парсить
**per-session**, `>1` файл сессии = kill; (2) heartbeat по wall-clock ≠ аудио → бить `STALL`
при 0 байт + ассертить рост PCM-байт (мьют-микрофон в Doze не должен проходить); (3) одиночная
сессия должна покрыть всё окно (early-end = kill); (4) device-скрипт обязан **fail-closed** (нет
лога → не exit 0). **Как применять:** verifier/native-spike-debugger — это чек-лист на любой
survival/Doze-тест, не только S1.

**[OEM, к проверке фаундером]** MIUI/Xiaomi-обход (battery=unrestricted + autostart ON) — пока
**гипотеза**, не подтверждён: device-прогон на ≥2 OEM (Xiaomi обяз.) — followup. FTL покрывает
~80%, НЕ OEM-киллеры → нужен физ-телефон. Чек-лист: `evidence/WAVE0-S1/device-matrix-checklist.md`.
