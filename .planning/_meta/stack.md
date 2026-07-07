# Технологический стек AIEAR (запиненный)

> ⚠️ Версии запинены намеренно — агенты дрейфуют по версиям. Менять только через ADR.

## Android (клиент)

| Слой | Выбор | Заметки |
|---|---|---|
| Язык | **Kotlin** | корутины, sealed/data-классы = плотный сигнал агенту |
| UI | **Jetpack Compose** | screenshot-тесты обязательны |
| Сборка | **Gradle** (AGP запинен) | `gradle/libs.versions.toml` — единственный источник версий |
| Фон | mic-FGS (`FOREGROUND_SERVICE_MICROPHONE`) | старт из foreground / CDM-исключение |
| minSdk | **31** (прод, ADR-014) | тиры: **Full** 14+ (screen-off фон) / **Degraded** 12–13 (захват при активном приложении/экране); спайки Wave-0 остаются на 34 |
| BT-автозапуск | `CompanionDeviceManager` + `ACTION_ACL_CONNECTED` | `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND` |
| Активация | Quick Settings tile + media-button hook | default-ассистент (`ROLE_ASSISTANT`) — только V1 |
| On-device STT | Vosk / whisper.cpp | офлайн + free-тир |
| On-device wake-word | Vosk-keyword (сначала) / Picovoice Porcupine (после добавления ключа) | спайк S6, wake-word «Эй, бадди», на взведённом сервисе |
| Тесты | JUnit + Compose UI test + **Gradle Managed Devices → Firebase Test Lab** | + `agent-device` (callstack) для on-device feedback |
| Lint | ktlint + detekt | |

## Backend

| Слой | Выбор |
|---|---|
| Язык/фреймворк | **Python 3.12 + FastAPI + Pydantic v2** — монолит, один деплой-юнит (ADR-013) |
| БД | PostgreSQL + **pgvector** (поиск по заметкам), Redis (очереди/кэш) — managed в Yandex Cloud |
| Хостинг | **Yandex Cloud** (`ru-central1`) — ФЗ-242; ко-локация со SpeechKit (ADR-013, выбор из тройки сделан) |
| Назначение | proxy к STT/LLM (ключи на сервере), RuStore-биллинг, sync, account+entitlement (device-token → VK/Яндекс ID, ADR-012) |
| Контракты | **`specs/_contracts/openapi.yaml` + `thought.schema.json`** — канонические; спеки ссылаются, не дублируют (ADR-013) |
| Деплой/CI | Docker-образ → **Yandex Serverless Containers**; отдельный `ci-backend` workflow (ruff + mypy --strict + pytest + build) |
| Lint/типы | ruff + mypy --strict |

## ИИ-пайплайн (RF-стек)

| Функция | Primary | Fallback / free | RF-оплата |
|---|---|---|---|
| STT | **Yandex SpeechKit** (стриминг ru; **WebSocket** `/v1/stt/stream` default, gRPC — только если S5 покажет провал p95 <2 с) | SaluteSpeech / Vosk / whisper.cpp | ✅ |
| LLM-структурирование | **GigaChat-2 Lite** (дефолт) за **сменным адаптером** backend-прокси; бенчмарк vs YandexGPT — evidence в F2 (Q1 закрыт) | — | ✅ |
| TTS (с V1) | Yandex / SaluteSpeech | — | ✅ |
| intl-LLM (Claude/GPT) | — | через KZ-юрлицо + прокси | 🔴 только V1.3 |

## Платежи

- **RuStore Pay SDK** (комиссия 15%) — in-store; старый BillingClient выключен 01.08.2026.
- **Свой эквайер** (CloudPayments / YooKassa) — внешний биллинг под 0%, СБП-рекуррент, чеки 54-ФЗ.
- Юрлицо: **ИП** (есть).

## Unit-экономика (драйвер квот)

- STT (Yandex SpeechKit) ≈ **39 ₽/час** (0,65 ₽/мин); + LLM-структурирование ≈ 5–20 ₽/час → **all-in ≈ 45–60 ₽/час**.
- Standard 399 ₽ → ~339 ₽ net → break-even **~5–8 ч/мес**.
- Pro 699 ₽ → ~594 ₽ net → break-even **~10–13 ч/мес**.
- **Квоты (ратифицированы grill 2026-07-07):** Free — **~30 мин облачного STT/мес** (после исчерпания on-device STT продолжает работать); Standard — **~4 ч/мес облачного STT + on-device микс**; Pro — ~8–10 ч (уточняется по Q2). Квоты ниже break-even → маржа >50% (гейт MVP-1).
- Рычаги: объёмные обязательства Yandex Cloud; on-device STT для free; дешёвый LLM на структурировании.

## CI/CD

GitHub Actions → lint/typecheck/unit → инструментальные на Gradle Managed Devices → Firebase Test Lab → security-сканы → **fastlane + `rustore-publish-gradle-plugin`**.

## Дистрибуция

RuStore (primary) + прямой APK + (опц.) AppGallery (free-only). iOS — V1.4 (см. PRD §V1, [`docs/research/ios-billing-options.md`](../../docs/research/ios-billing-options.md)).
