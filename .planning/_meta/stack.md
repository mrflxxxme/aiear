# Технологический стек AIEAR (запиненный)

> ⚠️ Версии запинены намеренно — агенты дрейфуют по версиям. Менять только через ADR.

## Android (клиент)

| Слой | Выбор | Заметки |
|---|---|---|
| Язык | **Kotlin** | корутины, sealed/data-классы = плотный сигнал агенту |
| UI | **Jetpack Compose** | screenshot-тесты обязательны |
| Сборка | **Gradle** (AGP запинен) | `gradle/libs.versions.toml` — единственный источник версий |
| Фон | mic-FGS (`FOREGROUND_SERVICE_MICROPHONE`) | Android 14+; старт из foreground / CDM-исключение |
| BT-автозапуск | `CompanionDeviceManager` + `ACTION_ACL_CONNECTED` | `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND` |
| Активация | Quick Settings tile + media-button hook | default-ассистент (`ROLE_ASSISTANT`) — только V1 |
| On-device STT | Vosk / whisper.cpp | офлайн + free-тир |
| On-device wake-word | Picovoice Porcupine / Vosk | спайк S6, на взведённом сервисе |
| Тесты | JUnit + Compose UI test + **Gradle Managed Devices → Firebase Test Lab** | + `agent-device` (callstack) для on-device feedback |
| Lint | ktlint + detekt | |

## Backend

| Слой | Выбор |
|---|---|
| Язык/фреймворк | **Python 3.12 + FastAPI + Pydantic v2** |
| БД | PostgreSQL + **pgvector** (поиск по заметкам), Redis (очереди/кэш) |
| Хранилище | объектное в **RF-облаке** (Yandex Cloud / VK Cloud / Selectel) — ФЗ-242 |
| Назначение | proxy к STT/LLM (ключи на сервере), RuStore-биллинг, sync, account+entitlement |
| Lint/типы | ruff + mypy --strict |

## ИИ-пайплайн (RF-стек)

| Функция | Primary | Fallback / free | RF-оплата |
|---|---|---|---|
| STT | **Yandex SpeechKit** (стриминг ru) | SaluteSpeech / Vosk / whisper.cpp | ✅ |
| LLM-структурирование | **GigaChat-2 Lite** (старт) ↔ YandexGPT (бенчмарк, Q-открытый) | — | ✅ |
| TTS (с V1) | Yandex / SaluteSpeech | — | ✅ |
| intl-LLM (Claude/GPT) | — | через KZ-юрлицо + прокси | 🔴 только V1.3 |

## Платежи

- **RuStore Pay SDK** (комиссия 15%) — in-store; старый BillingClient выключен 01.08.2026.
- **Свой эквайер** (CloudPayments / YooKassa) — внешний биллинг под 0%, СБП-рекуррент, чеки 54-ФЗ.
- Юрлицо: **ИП** (есть).

## Unit-экономика (драйвер квот)

- STT (Yandex SpeechKit) ≈ **39 ₽/час** (0,65 ₽/мин); + LLM-структурирование ≈ 5–20 ₽/час → **all-in ≈ 45–60 ₽/час**.
- Standard 399 ₽ → ~339 ₽ net → покрывает **~5–8 ч/мес** до нулевой маржи.
- Pro 699 ₽ → ~594 ₽ net → **~10–13 ч/мес**.
- Рычаги: объёмные обязательства Yandex Cloud; on-device STT для free; дешёвый LLM на структурировании.

## CI/CD

GitHub Actions → lint/typecheck/unit → инструментальные на Gradle Managed Devices → Firebase Test Lab → security-сканы → **fastlane + `rustore-publish-gradle-plugin`**.

## Дистрибуция

RuStore (primary) + прямой APK + (опц.) AppGallery (free-only). iOS — V1.4 (см. PRD §V1, [`docs/research/ios-billing-options.md`](../../docs/research/ios-billing-options.md)).
