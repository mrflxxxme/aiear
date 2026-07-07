# Глоссарий AIEAR

> Общий язык (ubiquitous language). Используй ровно эти термины в спеках, коде и хендофах.

## Продуктовый домен

| Термин | Значение |
|---|---|
| **AIEAR** | **Рабочее название** продукта (финальный бренд TBD — Q10). Wake-word — **«Buddy»**, произносится «Эй, бадди». |
| **Capture / захват** | Сессия записи короткой голосовой реплики пользователя. |
| **Thought / мысль** | Единица захвата: `{заголовок, тип, теги, нормализованный текст}` после ИИ-структурирования. |
| **Type / тип мысли** | Классификация: `задача` / `идея` / `напоминание` / `заметка`. |
| **Destination** | Куда экспортируется мысль из inbox. Must: Notes (inbox) / Календарь / Напоминания / Telegram / Share-Markdown; should: Obsidian (ADR-015). |
| **Inbox / inbox-first** | Каждая захваченная мысль **сначала** персистится во встроенное хранилище (Notes) приложения; destination'ы — асинхронные повторяемые экспорты из inbox (ADR-015). |
| **device-token** | Анонимный непрозрачный токен устройства — идентичность MVP-0 без регистрации; в MVP-1 опционально сливается с аккаунтом VK ID / Яндекс ID (ADR-012). |
| **CTA** | Предлагаемое действие («в напоминания на завтра 9:00»), подтверждаемое пользователем. |
| **Fixation / фиксация** | Доставка мысли в destination (North Star меряет именно дошедшие до destination). |
| **Voice command / голосовая команда** | Управление сессией голосом без касания экрана (F7): разметка, выбор destination, подтверждение/отмена. |

## Нативный Android

| Термин | Значение |
|---|---|
| **mic-FGS** | Foreground Service с типом `FOREGROUND_SERVICE_MICROPHONE` для фонового захвата при screen-off (полноценно — Full-tier, Android 14+). |
| **Tier Full / Degraded** | Тиры возможностей захвата при minSdk 31 (ADR-014): **Full** (Android 14+) — screen-off фоновый захват; **Degraded** (12–13) — захват при активном приложении/включённом экране. |
| **CDM** | `CompanionDeviceManager` — легальный автозапуск сервиса по подключению наушников из фона. |
| **Tile** | Quick Settings tile — ручной лаунчер сессии (0 батареи, 0 риска). |
| **media-button hook** | `KEYCODE_HEADSETHOOK` — in-session-триггер, когда AIEAR — активная медиа-сессия (ненадёжно по брендам). |
| **wake-word** | On-device hotword «Эй, бадди» (wake-word «Buddy») на **взведённом** сервисе пока наушники подключены (Vosk-keyword / Picovoice Porcupine). Спайк S6. |
| **default-ассистент** | `ROLE_ASSISTANT` + `VoiceInteractionService` — единственный путь к always-on hotword. Только V1 «Power Mode». |
| **OEM-killer** | Прошивка (MIUI/Honor/Transsion), агрессивно убивающая фоновые сервисы. Главный риск (R-OEM). |

## ИИ-пайплайн

| Термин | Значение |
|---|---|
| **STT** | Speech-to-Text. Primary: Yandex SpeechKit; fallback: SaluteSpeech; офлайн/free: Vosk/whisper.cpp. |
| **LLM-структурирование** | GigaChat/YandexGPT превращает транскрипт в `Thought`. |
| **WER** | Word Error Rate. Цель на ru-шуме `< 15%`. |
| **golden dataset** | Эталонный набор для оценки промптов `evaluator`'ом. |
| **adversarial-проход** | Попытка *опровергнуть* приёмку/промпт (а не подтвердить) на wave-гейте. |

## Харнесс / операционная модель

| Термин | Значение |
|---|---|
| **Wave** | Верхнеуровневая итерация роадмапа с go/no-go-гейтом (Wave 0 / MVP-0 / MVP-1 / V1 / V2+). |
| **Phase** | Фича или тесный кластер: 1 spec + 1 plan + 1 PR + 1 phase-аудит. |
| **Task** | Единица декомпозиции planner'а, исполняемая одним субагентом. |
| **Gate / гейт** | Точка остановки: phase-gate (аппрув фаундера tier 3+) или wave-gate (go/no-go по метрикам). |
| **Handoff** | Лёгкий MD+YAML-артефакт передачи работы между ролями. |
| **pinned-Opus** | Роль, которая никогда не падает на дешёвую модель (security, verifier, architect, planner). |
| **phase-close аудит** | Обязательный `AUDIT-REPORT.md` при закрытии фазы (8 линз, вкл. live-gold/evidence). |
| **live-gold** | golden/приёмочные сценарии против РЕАЛЬНЫХ сервисов/устройств (live STT/LLM, RuStore sandbox, FTL OEM), не моков — даёт реальные WER/pass-rate/выживаемость (ADR-010). |
| **evidence-бандл** | воспроизводимые доказательства прохождения в `specs/<wave>/evidence/<PHASE>/` (self-run, live-gold, FTL, coverage). Без него фаза не доходит до PR. |
| **AgentDB** | Семантическая память (claude-flow MCP): рекалл прошлых решений/паттернов. |
| **memory-curator** | Единственный писатель (single-writer) в STATUS/JOURNAL/memory + индекс AgentDB. |
| **escalation valve** | Клапан эскалации: `native-spike-debugger` или фаундер при застревании. |
