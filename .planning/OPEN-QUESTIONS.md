# OPEN-QUESTIONS — открытые вопросы

> Из PRD §18 + новые. Закрытие фиксируется в JOURNAL + (если меняет архитектуру) ADR.

| # | Вопрос | Владелец | Когда нужно | Статус |
|---|---|---|---|---|
| Q1 | GigaChat vs YandexGPT — финал по бенчмарку | Фаундер + `evaluator` | до MVP-0 **F2** | ✅ resolved: **GigaChat-2 Lite дефолт** + сменный адаптер; бенчмарк-evidence внутри F2, не блокер ([grill 2026-07-07 §3.2](_session-context/GRILL-2026-07-07-project-docs.md)) |
| Q2 | Реальные тарифы STT/LLM и объёмные скидки (базовая модель квот **зафиксирована** grill'ом: Free ~30 мин / Standard ~4 ч + on-device микс / Pro ~8–10 ч) | Фаундер | до контракта инфры | 🟡 open |
| Q3 | Набор OEM для device-sanity (топ РФ) | Фаундер | до **Wave 0** | ✅ resolved: ратифицировано — **FTL Pixel/Samsung + физ-Xiaomi обязателен + Docker-эмулятор** (smoke-ярус) ([grill 2026-07-07 §4.2](_session-context/GRILL-2026-07-07-project-docs.md)) |
| Q4 | Формат согласия на запись (юр-проверка) | Legal | до MVP-1 **M4** | 🟡 open |
| Q5 | KZ-юрлицо для intl-ИИ | Фаундер/Legal | до **V1.3** | 🟡 open |
| Q6 | Исход спайка **S6** (wake-word едет ли в MVP-0.x) | `native-spike-debugger` + фаундер | по итогам Wave 0 | 🟡 open |
| Q7 | Cost-caps в `$` — подтвердить/поправить числа из ORIION-дефолтов | Фаундер | до первого автономного прогона | 🟡 open |
| Q8 | Подключение claude-flow MCP (AgentDB) — когда корпус памяти оправдает | Фаундер | после первых фаз MVP-0 | 🟡 open |
| Q9 | Egress для Android-сборок в автономных сессиях | Фаундер | до след. Android-фазы (S2/S3, F1) | ✅ resolved: решение — **allowlist `maven.google.com`/`dl.google.com` + FTL-креды (GCP service-account) + Docker-эмулятор фаундера**; исполнение — ONBOARDING-SECRETS.md ([grill 2026-07-07 §4.2](_session-context/GRILL-2026-07-07-project-docs.md)) |
| Q10 | **Бренд/нейминг финальный** (AIEAR — рабочее название) | Фаундер | до стор-листинга **и до тренировки wake-word** | 🟡 open |

## Как закрывать

1. Агент, упёршийся в открытый вопрос, эмитит `escalation.*` или `task.unclear` и **не додумывает**.
2. Фаундер/ответственный решает → `memory-curator` пишет в JOURNAL, при необходимости `architect` заводит ADR.
3. Строка переводится в `✅ resolved` со ссылкой на ADR/JOURNAL.
