---
phase: WAVE0-S1
pipeline: pipeline-android (spike variant)
owner: native-spike-debugger
adr_refs: [ADR-002, ADR-010]
acceptance_refs: [S1-AC1, S1-AC2, S1-AC3]
tier: 4
---

# PLAN — WAVE0-S1 (mic-FGS переживает screen-off)

> Стадия 2. `planner` декомпозирует спайк → задачи + DAG + model-хинты + Evidence-план.
> Канон: `02-PIPELINE.md` (спайк-пайплайн), `07-VERIFICATION-EVIDENCE.md`.

## Пайплайн фазы
`planner` → `android-engineer`/`native-spike-debugger` (impl) → `reviewer` ∥ `reviewer-security`
→ `verifier` (self-run + EARS↔тест + evidence-бандл) → `architect` (phase-close аудит, 8 линз)
→ `memory-curator` (STATUS/JOURNAL/memory/README + draft-PR) → **СТОП на гейте (no merge)**.

## Задачи и DAG

| # | Задача | Артефакт | Зависит | model_hint | AC |
|---|---|---|---|---|---|
| **T1** | Gradle-scaffold: settings/root build, `libs.versions.toml` (пины), `gradle.properties`, wrapper 8.14.3, ktlint(`.editorconfig`)+detekt(`config/detekt`) | корень + `gradle/` | — | Sonnet | — |
| **T2** | `app/`-скелет: модуль `app/build.gradle.kts` (+`managedDevices`), `AndroidManifest.xml` (perms + `<service foregroundServiceType=microphone>`, **без** boot-receiver), `res/` (тема/строки/иконка), `AiearApp`, `MainActivity`, `ui/theme/*`, `ui/CaptureScreen` | `app/src/main` | T1 | Sonnet | — |
| **T3** | **mic-FGS ядро**: `capture/service/MicForegroundService` (FGS-mic, `AudioRecord`-цикл → PCM, heartbeat 10 с, `START_STICKY`, лог прерывания), `NotificationHelper` (ongoing mic-notif), `HeartbeatLogger` (чистые `format()`/`maxGapMs()`) | `app/.../capture` | T2 | **Opus** (нативное/конкурентное) | AC2, AC3 |
| **T4** | Старт-поверхность: кнопка Start/Stop в `CaptureScreen` → `startForegroundService` из foreground (единственный легальный путь, ADR-002) | `ui/CaptureScreen` | T2,T3 | Sonnet | — |
| **T5** | Юнит-тест эвалуатора heartbeat (`format` стабилен/greppable; `maxGapMs` ловит дыру) — единственный поведенческий тест, исполнимый в песочнице | `app/src/test` | T3 | Sonnet | AC1(proxy) |
| **T6** | Инструментальный тест выживания `MicFgsSurvivalTest` + `SpikeTestConfig` (param `durationMin` 2↔60, `forceDoze`): start→`KEYCODE_SLEEP`→`force-idle`→assert непрерывность; **никогда не проходит молча** — дыра парсится+логируется | `app/src/androidTest` | T3 | **Opus** | AC1, AC2, AC3 |
| **T7** | Founder-run-kit: `run-on-device.sh` (grant→launch `durationMin=60`→sleep→force-idle→pull), парсер heartbeat, `device-matrix-checklist.md`, GMD/FTL команды | `evidence/WAVE0-S1/` | T6 | Sonnet | AC1–AC3 (deferred) |
| **T8** | **Self-run + evidence-бандл**: доустановить SDK → `ktlintCheck detekt testDebugUnitTest assembleDebug assembleDebugAndroidTest` → вывод в `selftest-android.txt`; `verify-acceptance.md` (EARS↔тест), `evidence-gap.md` | `evidence/WAVE0-S1/` | T1–T7 | **Opus** (verifier) | AC1–AC3 |

DAG: T1→T2→{T3→(T4,T5,T6→T7)}→T8. T4/T5 параллельны после T3.

## Model-хинты (по `model-routing.md`)
Нативное ядро (T3) и инструментальный тест (T6) + verifier (T8) — **Opus** (главный риск EARAI,
эскалация native/FGS/OEM = pinned-Opus). Scaffold/UI/юнит (T1,T2,T4,T5,T7) — Sonnet (идиоматичное).
`reviewer-security` (mic/RECORD_AUDIO/permissions) и `architect` (аудит) — **pinned-Opus**.

## Acceptance ↔ артефакт (1:1, проверит verifier)
- **S1-AC1** (≥60 мин screen-off) → `MicFgsSurvivalTest.capture_survives_screenOff_and_doze`
  (`durationMin=60`), доказательство = непрерывность heartbeat-лога. *В песочнице — только
  юнит-проверка эвалуатора непрерывности; device-прогон → фаундер.*
- **S1-AC2** (постоянное FGS-уведомление mic) → `NotificationHelper` (`setOngoing`, FGS-type
  microphone) + ассерт `micNotificationPresent()` в тесте.
- **S1-AC3** (Doze: сохранить ИЛИ корректно зафиксировать прерывание) → ветка `forceDoze` +
  логирование `READ_ERR`/`EXC`/гэпов в `HeartbeatLogger`; тест парсит и репортит точку разрыва.

## Evidence & live-gold план (ADR-010)
- **Self-run (verifier, эта сессия):** попытка установки cmdline-tools+platform-35+build-tools
  через прокси; при успехе — реальный вывод lint/unit/assemble в `selftest-android.txt`. При
  блокировке прокси → `evidence_gap: needs-android-sdk-in-sandbox`, откат на структурную проверку.
- **Live-gold (deferred, фаундер):** `run-on-device.sh` на ≥2 физ-OEM (вкл. Xiaomi) — screen-off
  ≥60 мин + Doze + low-RAM, логи в `device-logs/`. `evidence_gap: needs-device` (+ опц.
  `needs-FTL-creds`). **GO** = AC1 зелёный на ≥2 OEM (Xiaomi обяз.).
- **Бандл:** `selftest-android.txt`, `verify-acceptance.md`, `evidence-gap.md`, `run-on-device.sh`,
  `device-matrix-checklist.md`, (позже) `device-logs/`, `ftl-runs.md`.

## Запреты (ADR-002 — блок на ревью)
`BOOT_COMPLETED` mic-старт · AccessibilityService-автоматизация · silent-audio Now Playing ·
секреты в репо. Старт сервиса — только из foreground-Activity.
