---
spike: WAVE0-S1
title: Фоновый mic-FGS переживает screen-off
status: approved
blocking: true
owner_role: native-spike-debugger
adr_refs: [ADR-002]
prd_refs: ["§8 Wave 0 S1", "§10.1"]
---

# S1 — mic-FGS переживает screen-off ≥60 мин

## Гипотеза
Foreground Service с `FOREGROUND_SERVICE_MICROPHONE` (Android 14+) продолжает захват аудио после погасания экрана ≥60 мин на реальном устройстве, без прерывания ОС/Doze. Это фундамент F1 (захват) и M1 (встречи). Риск: OEM-киллеры и Doze рвут сервис.

## Acceptance criteria (EARS)
- **S1-AC1** — WHEN запись запущена и экран гаснет, THE SYSTEM SHALL продолжать захват аудио без прерывания **≥ 60 мин** на эталонном устройстве.
- **S1-AC2** — WHILE сервис активен в фоне, THE SYSTEM SHALL держать постоянное уведомление FGS (тип microphone), как требует ОС.
- **S1-AC3** — IF ОС переводит устройство в Doze, THEN THE SYSTEM SHALL сохранять захват (mic-FGS исключён из Doze-ограничений микрофона) ИЛИ корректно фиксировать прерывание для последующей диагностики.

## Метод
1. Минимальный сервис: старт mic-FGS из foreground, пишет аудио в файл, логирует heartbeat каждые 10 сек.
2. Запуск → гашение экрана → ожидание 60+ мин (с принудительным Doze: `adb shell dumpsys deviceidle force-idle`).
3. Измеряем: непрерывность heartbeat, целостность аудио, момент любого киллa (logcat).

## Device-матрица
| Устройство | OEM | Фокус | Результат |
|---|---|---|---|
| Pixel 8 | Google | базовая выживаемость + Doze | — |
| Redmi/POCO | Xiaomi (MIUI) | OEM-киллер фона, battery-restriction | — |
| Galaxy A/S | Samsung | оптимизация батареи | — |

## Go / No-Go
- **GO:** AC1 зелёный на ≥2 OEM (обязательно вкл. Xiaomi).
- **Условный:** если MIUI убивает — задокументировать обход (battery-unrestricted онбординг-чек, см. `memory/android-oem.md`) и переснять. NO-GO всего MVP, если ни один обход не держит 60 мин на массовом OEM.

## Что решает исход
Без зелёного S1 не стартуют F1/M1. Это «гейт взлёта» №1.

## Эскалация
MIUI-специфика → `native-spike-debugger` через agent-device + Don't-Kill-My-App матрица; фундаментальный блок → фаундер (физ-устройство).
