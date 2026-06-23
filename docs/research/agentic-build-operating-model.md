# EarAI — Операционная модель агентной разработки (соло-фаундер + ИИ-агенты)

> Цель: реализовать нативное Android-приложение силами ИИ-агентов, фаундер — ревьюер и «руки между фазами», не кодер.
> Вывод ресёрча: **выполнимо в 2026**, при условии spec-driven харнесса + автоматического on-device цикла верификации. Главный риск (нативный фон/OEM) снимается не «человеком, который это кодит», а **тем, что агенту дают обратную связь с реального устройства**.

---

## 0. Ключевой инсайт

ИИ-агенты в 2026 хорошо пишут **идиоматичный Kotlin** (корутины, Compose, Room) — типизированный модульный Android даёт агенту плотный сигнал и меньше «тихих» ошибок, чем динамические стеки. Слабое место одно: **агенты плохо взаимодействуют с запущенным приложением** (фоновые сервисы, OEM-поведение). Решение — не нанимать кодера, а **замкнуть агенту цикл «gather → act → verify» через реальное устройство**.

---

## 1. Харнесс / структура репозитория (oriion-совместимая, SDD)

```
CLAUDE.md                  # канонический контекст + конвенции + security-правила
.planning/
  PROJECT.md               # видение/USP/решения (из этого PRD)
  roadmap/                 # волны с acceptance-критериями (EARS-стиль)
  decisions/               # ADR по техрешениям из ресёрча
  STATUS.md                # роллинг-статус фаз
.claude/agents/            # специализированные сабагенты (ниже)
specs/<feature>/           # per-feature: user stories + EARS-критерии + API-контракты + edge cases
```

**Spec-Driven Development:** spec → plan → tasks → implement, исполняемо агентами. Спека — первичный артефакт, из неё генерятся код+тесты+доки. Критерии приёмки — **EARS** (строгий родственник Gherkin), чтобы агенту было нечего «додумывать».

**Сабагенты (`.claude/agents/`):** `android-engineer`, `backend-engineer`, `test-engineer`, `native-spike-debugger`, `reviewer`, `release-manager`, `designer`. Стартовые шаблоны — VoltAgent `kotlin-specialist` / `mobile-developer`.

---

## 2. Агентный цикл на тикет (gather → act → verify)

1. **Спека** (одобрена фаундером) → агент декомпозирует в задачи.
2. **Имплементация** (Kotlin / FastAPI).
3. **Автоматические гейты качества (CI, GitHub Actions):**
   - build + `ktlint`/`detekt` + typecheck
   - unit-тесты (JUnit)
   - **инструментальные тесты на Gradle Managed Devices → Firebase Test Lab** (реальные + виртуальные устройства, smart-sharding)
   - screenshot-тесты (Compose)
4. **При фейле** — агент итерирует; на злых нативных кейсах подключается **agent-device CLI** (callstack): агент открывает приложение на устройстве, инспектирует UI/логи, собирает доказательства — закрывает разрыв «агент не видит, что реально происходит».
5. **Зелёный гейт → PR → ревью фаундера** на фазовом стыке.

---

## 3. Верификация — как снимается нативный риск (самое важное)

| Инструмент | Что закрывает |
|---|---|
| **Gradle Managed Devices + Firebase Test Lab** | автоматический прогон инструментальных тестов по матрице реальных устройств в CI — так агент «видит», переживает ли mic-FGS погашенный экран на устройстве X |
| **agent-device (callstack CLI)** | даёт кодящему агенту реальный on-device feedback-loop (открыть/инспектировать/кликать/собрать логи) |
| **Firebase App Testing agent (Gemini)** | NL-цели тестов, ИИ навигирует приложение |
| **Don't-Kill-My-App OEM-матрица** | чек-лист по Xiaomi/Huawei/Samsung/Oppo — единственное, что требует **физической** проверки фаундером (облако покрывает не всё) |

---

## 4. Где фаундер остаётся «руками» (минимизировано, между фазами)

1. **Пишет/одобряет спеки и ADR** — это стратегия (по сути, этот PRD).
2. **Ревью PR на фазовых гейтах** + приёмка по acceptance-критериям.
3. **Физический sanity-pass** на 2–3 реальных OEM-телефонах по фоновому поведению (облачные фермы покрывают ~80%, но не OEM-убийц фона).
4. **Разовая настройка, которую агент не сделает:** RuStore dev-аккаунт (ИП), keystore/подпись, ключи Yandex Cloud / GigaChat, billing-sandbox, идентичность для сабмита.
5. **Клапан эскалации:** если агент зациклился на нативном баге > N попыток — фаундер пейрит или зовёт `native-spike-debugger`.

---

## 5. Риск-контроли агентной нативной сборки

- **Front-load Wave 0 спайков** (5 «гейтов взлёта»: mic-FGS при screen-off → BT-autostart → активация tile/media-button → RuStore Pay SDK sandbox → STT-стриминг латентность). Агент пробует, фаундер валидирует на устройстве.
- **Мелкие типизированные модули** (sealed/data-классы = плотный сигнал агенту).
- **Запиненный тулчейн** (версии AGP/Gradle/Kotlin) — агенты дрейфуют по версиям.
- **Секреты вне контекста агента** (env/CI-secrets).
- **Бюджет токенов/прогонов** заменяет бюджет зарплат.

---

## 6. Рекомендованный тулчейн

| Слой | Выбор |
|---|---|
| Оркестрация | Claude Code + сабагенты (oriion-паттерн) + SDD-харнесс (Spec-Kit-стиль) |
| Сборка | Kotlin + Jetpack Compose + Gradle; backend Python/FastAPI (как oriion, agent-friendly) |
| Тест/верификация | JUnit + Compose UI test + Gradle Managed Devices + Firebase Test Lab + agent-device |
| CI/CD | GitHub Actions → lint/test/instrumentation → **fastlane + `rustore-publish-gradle-plugin`** в RuStore |
| Девайс-ферма | Firebase Test Lab (облако) + 2–3 физических OEM-телефона (фаундер) |

---

## 7. Реframe бюджета

Вместо ~3,6 млн ₽ зарплат → **токены агентов (Claude Code) + Firebase Test Lab + API (Yandex/GigaChat) + инфра (Yandex Cloud) + 2–3 тест-телефона + резерв на эскалацию.** Кэш-burn кратно ниже; «валюта» проекта — время фаундера на ревью и чёткость спек.

---

## Источники
- [Claude Code для Android (2026)](https://vmobify.com/blog/claude-code-android-development) · [Production Android workflows](https://vmobify.com/blog/production-android-workflows-claude)
- [VoltAgent kotlin-specialist subagent](https://github.com/VoltAgent/awesome-claude-code-subagents/blob/main/categories/02-language-specialists/kotlin-specialist.md)
- [Gradle Managed Devices](https://developer.android.com/studio/test/managed-devices) · [Firebase Test Lab](https://firebase.google.com/docs/test-lab)
- [agent-device (callstack)](https://github.com/callstack/agent-device)
- [Spec-Driven Development с Claude Code](https://thebcms.com/blog/spec-driven-development) · [Как писать спеку для агентов (Addy Osmani)](https://addyosmani.com/blog/good-spec/)
- [rustore-publish-gradle-plugin](https://github.com/cianru/rustore-publish-gradle-plugin) · [fastlane-plugin-rustore](https://github.com/stfbee/fastlane-plugin-rustore)

## Флаги неопределённости
- ⚠️ OEM-фон (Xiaomi/Honor/Transsion) — единственная зона, где облачная верификация неполна; держать физические устройства.
- ⚠️ Лимиты/цены Firebase Test Lab при частых прогонах — заложить в бюджет токенов/CI.
- ⚠️ Зрелость agent-device под специфичные фоновые сценарии — проверить на спайке Wave 0.
