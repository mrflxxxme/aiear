# Verify — acceptance ↔ test (WAVE0-S1)

**Role:** verifier (pinned-Opus) · **Date:** 2026-06-24 · **Canon:** ADR-010, `07-VERIFICATION-EVIDENCE.md`
**Gate inputs:** `selftest-android.txt`, `evidence-gap.md`, independent verifier re-run (below).

> «Зелёное по доказательству, не по утверждению.» Здесь каждый EARS-критерий сведён 1:1 к тесту/
> артефакту с честным статусом: что доказано в песочнице vs что вынесено на устройство фаундера.

## Independent verifier re-run (не доверяю отчёту имплементера — перепроверено)
Чистая комната: извлёк **точные** тела `HeartbeatLogger.format()`/`maxGapMs()` из
`app/src/main/.../HeartbeatLogger.kt` + **точные** ассерты из `app/src/test/.../HeartbeatLoggerTest.kt`,
скомпилировал `kotlinc 2.0.21` (K2JVMCompiler), прогнал на `JUnit 4.13.2` (Maven Central):

```
=== compile (kotlinc 2.0.21) ===  com/aiear/capture/HeartbeatSeam.class + HeartbeatSeamTest.class
=== run (JUnitCore) ===           JUnit 4.13.2 · ..... · OK (5 tests)
```
→ **5/5 OK** воспроизведено независимо. Детектор непрерывности (`maxGapMs`) ловит разрыв
(`[0,10k,20k,60k,70k]→40000`), пустой/единичный → 0, формат строки точный/greppable.

## EARS ↔ тест/артефакт

| Критерий | Артефакт / тест | В песочнице | На устройстве (фаундер) |
|---|---|---|---|
| **S1-AC1** — захват без прерывания ≥60 мин после screen-off | `MicFgsSurvivalTest.capture_survives_screenOff_and_doze` (`durationMin=60`); метрика = `HeartbeatLogger.maxGapMs ≤ 15 000 мс` по heartbeat-логу | ⚠️ **частично**: компилируется; детектор непрерывности (`maxGapMs`) проверен 5/5 как прокси-логика. Сам 60-мин прогон — нельзя (нет девайса/KVM) | ⏳ **deferred** — `run-on-device.sh` на ≥2 OEM |
| **S1-AC2** — постоянное FGS-уведомление (тип microphone) | `MicForegroundService.startCapture()`: `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)` ДО `AudioRecord`; `NotificationHelper.setOngoing(true)`; ассерт `micNotificationPresent()` | ⚠️ **структурно**: код+манифест верны (ревью+парс), тип совпадает manifest↔`startForeground` | ⏳ **deferred** — ассерт уведомления в survival-тесте |
| **S1-AC3** — Doze: сохранить захват ИЛИ корректно зафиксировать прерывание | `forceDoze` → `dumpsys deviceidle force-idle`; широкий `catch(Throwable)` → `READ_ERR`/`EXC`-beat; `locateBreak()` указывает точку разрыва (тест **никогда не проходит молча**) | ⚠️ **структурно**: ветка Doze + логирование прерывания присутствуют и корректны | ⏳ **deferred** — Doze-прогон на устройстве |

## Что доказано в этой сессии (live, не assertion)
- ✅ Gradle wrapper 8.14.3 реальный, исполняется (`./gradlew --version`).
- ✅ Version-catalog wiring корректен (алиас резолвится в `com.android.application:8.7.3`; падение **только** на сетевом fetch).
- ✅ **Поведение**: pure heartbeat-seam — 5/5 (независимо перепроверено verifier'ом).
- ✅ Синтаксис всех 12 Kotlin-источников чист (0 syntax-ошибок; остальное — отсутствующий android-classpath).
- ✅ ADR-002 conformance (ревью): нет BOOT_COMPLETED/Accessibility/silent-audio; старт только из foreground-Activity; `exported=false`.
- ✅ Секретов/PII/сети в спайке нет (скан).

## Evidence_gap (объявлено, ADR-010 — не тихий скип)
1. **`needs-google-maven-egress`** — полный Gradle-build (`ktlintCheck`/`detekt`/`testDebugUnitTest`/
   `assembleDebug`/`assembleDebugAndroidTest`) заблокирован 403-политикой на Google Maven.
   Детали и unblock-пути — `evidence-gap.md` Gap 1. Подтверждено proxy-status (authoritative).
2. **`needs-device`** — реальный S1-AC1/AC2/AC3 (≥60 мин screen-off + Doze + непрерывность на
   реальном OEM) требует физ-устройства/FTL. Founder-run-kit: `run-on-device.sh` +
   `device-matrix-checklist.md`. Детали — `evidence-gap.md` Gap 2.

## Вердикт verifier
**conditional-pass** → к гейту как **`pass-with-followups`** ПРИ условии, что фаундер принимает
оба evidence_gap. Код/структура/поведенческий прокси — зелёные по доказательству. Финальное
S1-GO (AC1 на ≥2 OEM, Xiaomi обяз.) **не подтверждено в этой сессии** и вынесено фаундеру —
это явный followup, не скрытый скип. «Компилируется» ≠ «переживает screen-off» — и это honored.
