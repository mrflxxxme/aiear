# PLAN — WAVE0-S2 (CDM-автозапуск захвата по BT из фона)

> Сгенерирован раннером `/autonomy:run WAVE0-S2` (ADR-011 D6). Spec: [`S2-cdm-bt-autostart.md`](./S2-cdm-bt-autostart.md). Решения залогированы в `.planning/_session-context/DECISIONS-LOG.md`.

## Discuss — резолв форков (D4)

Все форки **agent-owned** (архитектура/имплементация в рамках ADR-002 + memory/cdm-bt). **Эскалаций 0** — ни продукт/рынок, ни новый tripwire-подход (CDM как путь уже зафиксирован spec'ом + ADR-002). Задняя растяжка сработает на мёрже (native) → ack.

| Форк | Класс | Решение (лог) |
|---|---|---|
| Фон-триггер: `CompanionDeviceService.onDeviceAppeared` vs raw `ACTION_ACL_CONNECTED` receiver | impl/arch (owned) | CompanionDeviceService presence-observation — санкционированный API-34 путь; raw ACL ненадёжен из фона |
| Цель старта: прямой mic-FGS vs non-mic «взвод» → chain | impl (owned) | Прямой mic-FGS из onDeviceAppeared (тип microphone); non-mic взвод — документированный OEM-fallback |
| S2-AC3 стоп-семантика | impl (owned) | onDeviceDisappeared → `stopService` (→ onDestroy → чистый стоп); не держать мик без наушников |

`wide_fork:` — фон-триггер потенциально широкий, но recommended default взят по memory/ADR-002 (не гоняли judge-панель: пространство сужено spec'ом до CDM; выбор внутри CDM однозначен). Device-результат — арбитр (Go/No-Go), не judge-панель.

## Задачи

| # | Задача | Роль | Deliverables | Acceptance |
|---|---|---|---|---|
| T1 | CDM-ассоциация + presence-observation | android-engineer | `capture/companion/CompanionPairing.kt` | S2 Method §1 |
| T2 | Фон-триггер сервис | android-engineer | `capture/companion/CompanionCaptureService.kt` | S2-AC1/AC2/AC3 (wiring) |
| T3 | Manifest: permissions + feature + service | android-engineer | `AndroidManifest.xml` | S2-AC2 (legal bg-start surface) |
| T4 | UI онбординг сопряжения | android-engineer | `MainActivity.kt`, `ui/CaptureScreen.kt`, `strings.xml` | S2 Method §1 (one-time pairing) |
| T5 | Instrumented wiring-тест (start/stop контракт) | android-engineer/verifier | `androidTest/.../CompanionAutostartTest.kt` | S2-AC3 wiring; CDM feature+service declared |
| T6 | Device run-kit + evidence manifest | verifier | `evidence/manifest.json`, `specs/.../run-on-device.sh` | D3 device_survival gate |
| T7 | Security-ревью нативной/permission-поверхности | reviewer-security | ревью-вердикт | tripwire native |

## Гейты

- **Local (доступно):** ktlint + detekt + unit + assemble — через **ci-android** на PR (Android SDK в этом окружении отсутствует; ci-android ставит SDK, как на S1).
- **device_survival (D3-native, founder evidence_gap):** реальный CDM connect/disconnect на ≥2 OEM (Xiaomi обяз.) → `run-on-device.sh` эмитит `evidence/device_survival.json` (PASS) → ci-evidence зелёный. **До этого ci-evidence RED BY DESIGN** — нативный спайк не мёржится без device-доказательства (ADR-011 D3-native).

## Merge posture (rails-first)

Auto-merge ВЫКЛЮЧЕН. Раннер доводит до зелёного (кроме device_survival — founder-gap), открывает PR, tripwire classify → native → RUN-QUEUE `ack-needed`. Мёрж — фаундер `/autonomy:ack` **после** закрытия device_survival на ≥2 OEM.

## Что НЕ входит (scope guard)

Re-observation после reboot, multi-device, реальная запись в облако — F1. Fallback-цепочка (non-mic взвод + tile + media-button) — S3. START_NOT_STICKY auto-restart — F1.
