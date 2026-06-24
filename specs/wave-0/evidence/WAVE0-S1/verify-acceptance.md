# Verify — acceptance ↔ test (WAVE0-S1)

**Role:** verifier (pinned-Opus) · **Date:** 2026-06-24 · **Canon:** ADR-010, `07-VERIFICATION-EVIDENCE.md`
**Gate inputs:** `selftest-android.txt`, `evidence-gap.md`, independent verifier re-run (below).

> «Зелёное по доказательству, не по утверждению.» Здесь каждый EARS-критерий сведён 1:1 к тесту/
> артефакту с честным статусом: что доказано в песочнице vs что вынесено на устройство фаундера.

## Independent verifier re-run (не доверяю отчёту имплементера — перепроверено)
Чистая комната: извлёк **точные** тела `HeartbeatLogger.format()`/`maxGapMs()`/`maxGapWithinSessions()`
из `app/src/main/.../HeartbeatLogger.kt` + **точные** ассерты из `app/src/test/.../HeartbeatLoggerTest.kt`,
скомпилировал `kotlinc 2.0.21` (K2JVMCompiler), прогнал на `JUnit 4.13.2` (Maven Central):

```
=== run (JUnitCore) ===  JUnit 4.13.2 · ....... · OK (7 tests)
```
→ **7/7 OK** воспроизведено независимо (5 исходных + 2 на per-session-фикс). Детектор непрерывности
ловит разрыв, пустой/единичный → 0; **per-session** `maxGapWithinSessions` не смешивает kill+restart
(t= сброс) в малую дельту. Полный лог — `harden-rerun.txt`.

## Post-audit hardening (revision loop — AUDIT нашёл 4 major false-PASS)
Архитектор-аудит выявил, что девайс-харнесс мог дать **ложный green**. Исправлено в этой сессии и
ре-верифицировано механически (`harden-rerun.txt`): START_NOT_STICKY (kill=терминальный); STALL-beat
+ ассерт роста PCM-байт (мьют-микрофон не пройдёт); per-session-парсинг + детект restart/early-end;
`run-on-device.sh` **fail-closed** (нет лога/reset/early-end → ненулевой код); AC2 — наш канал + во
время screen-off; minSdk-precondition в чек-лист. Инструмент теперь не лжёт; сам device-прогон —
по-прежнему deferred (`needs-device`).

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
