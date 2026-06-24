# Grill — WAVE0-S1 (mic-FGS переживает screen-off)

> Стадия 1 пайплайна (discuss, до кода). Фиксирует scope, относящиеся открытые вопросы
> с **рекомендованными ответами** (приняты как ЗАДОКУМЕНТИРОВАННЫЕ ДОПУЩЕНИЯ — на ратификацию
> фаундера в PR) и границу `evidence_gap`. Канон: `agent-handbook/02-PIPELINE.md`, ADR-010.

**Дата:** 2026-06-24 · **Фаза:** WAVE0-S1 · **Owner:** native-spike-debugger · **Tier:** 4 (native-background)
**Спека:** `specs/wave-0/S1-mic-fgs-screenoff.md` · **ADR:** ADR-002, ADR-010

## Scope (что доказывает спайк)
Foreground Service с `FOREGROUND_SERVICE_MICROPHONE` (Android 14+) продолжает захват аудио
**≥60 мин после погасания экрана**, в т.ч. под Doze, без киллa ОС/OEM. Это фундамент F1/M1 —
«гейт взлёта №1». Спайк доказывает **выполнимость**, не шлёт продукт.

В scope: минимально-достаточный, но **реальный `app/`-скелет** (решение фаундера) c mic-FGS
внутри (переиспользуемо для F1); инструментальный тест выживания (smoke 2 мин ↔ full 60 мин);
конфиг Gradle Managed Devices / FTL; founder-run-kit; self-run evidence в пределах песочницы.

Вне scope: продуктовый UI, STT/LLM, биллинг, CDM-автозапуск (S2), tile (S3). Ключи
Yandex/GigaChat для S1 **не нужны** — записываем PCM локально, никакой ПД-обработки.

## Открытые вопросы — резолюции (допущения на ратификацию)

| # | Вопрос | Рекоменд. ответ (= допущение) | Статус |
|---|---|---|---|
| **Q3** | Набор OEM для device-sanity | Pixel 8 (Google: базовая выживаемость + Doze), Redmi/POCO (Xiaomi/MIUI: OEM-киллер, battery-restriction), Galaxy A/S (Samsung: оптимизация батареи) — прямо из device-матрицы спеки. **Xiaomi обязателен для GO.** | 🟡 на ратификацию |
| env | Креды Firebase Test Lab / GCP в этой сессии | **Нет** (проверено: `gcloud` отсутствует, `GOOGLE_APPLICATION_CREDENTIALS` пуст). Live-gold на реальных OEM в облаке невозможен этой сессией. | факт |
| env | Android-тулчейн в песочнице | JDK 21 + Gradle 8.14.3 есть; **Android SDK нет** (`ANDROID_HOME` пуст), **`/dev/kvm` нет** → эмулятор невозможен. Self-run попытается доустановить cmdline-tools (см. ниже). | факт |
| scaffold | Объём скелета | **Полный `app/`-скелет** (решение фаундера): Application + Compose-тема + структура пакетов + mic-FGS внутри. | ✅ решено |
| branch | Ветка разработки | `claude/happy-franklin-jwt600` (назначенная сессии harness-правилами), PR → `main`. Имя `wave/wave-0-s1-mic-fgs` из промпта — концептуальное имя фазы. | 🟡 на ратификацию |
| версии | Пины AGP/Kotlin/SDK | `stack.md` делегирует версии в `libs.versions.toml`, но чисел не даёт → пинуем (AGP 8.7.3 / Kotlin 2.0.21 / Gradle 8.14.3 / compileSdk 35 / targetSdk 34 / minSdk 34). | 🟡 на ратификацию |
| minSdk | Минимальная версия | **minSdk 34** для спайка (нужна enforcement FGS-типа API-34). Продуктовый minSdk (охват рынка РФ) — отдельное продуктовое решение позже. | 🟡 на ратификацию |

## Граница evidence_gap (ADR-010 — без «зелёного по утверждению»)

**Можно в этой сессии (если SDK доустановится через прокси):** `ktlintCheck`, `detekt`,
`testDebugUnitTest` (юнит-тест эвалуатора heartbeat), `assembleDebug`,
`assembleDebugAndroidTest` (компиляция инструментального теста). Это **частичное** evidence:
«компилируется + статика чиста», не «переживает screen-off».

**Нельзя в облаке (объявленный `evidence_gap`):**
- `connectedDebugAndroidTest` / GMD-прогон — нужен девайс/эмулятор (нет `/dev/kvm`).
- **Сам S1-AC1/AC2/AC3** (≥60 мин screen-off + Doze + непрерывность heartbeat на реальном OEM)
  — нужен физический телефон или FTL. → `evidence_gap: needs-device` (+ опц. `needs-FTL-creds`).
- Если прокси заблокирует загрузку SDK → второй `evidence_gap: needs-android-sdk-in-sandbox`,
  откат на структурную верификацию. **Честно, без фейк-грина.**

Закрытие gap: founder-run-kit (`run-on-device.sh` + парсер + чек-лист) гоняется фаундером на
≥2 физ-OEM (вкл. Xiaomi), логи кладутся в `evidence/WAVE0-S1/`.

## Критерий «готово» для гейта
S1-AC1..AC3 подтверждены воспроизводимым evidence на **≥2 OEM (обязательно Xiaomi)** — **на
фаундере** (физ-устройства). Эта сессия доводит до гейта: runnable-артефакт + тесты + скрипты
+ доступное статическое evidence + явный `evidence_gap` и предложение принять его (verdict
`pass-with-followups`). NO-GO всего MVP — только если ни один обход не держит 60 мин на массовом
OEM (решает фаундер по логам).

## Hard-блокеры
Нет полного блокера на эту сессию: артефакт и скрипты строятся без кредов/девайсов; только
**финальное** доказательство выживания заблокировано (device) и явно вынесено фаундеру.
