<!-- AUTO-MAINTAINED by memory-curator at every phase-close. Manual edits may be overwritten. -->
# STATUS — роллинг-статус AIEAR

**Обновлено:** 2026-06-23 · **Текущая волна:** Wave 0 (спайки риска, блокирующая) · **Активная фаза:** — (ожидает старта S1)

## Сводка волн

| Wave | Цель | Статус | Гейт |
|---|---|---|---|
| **Wave 0** | Спайки риска S1–S6 (5 «гейтов взлёта» + 1 оценочный) | 🔜 готов к старту | S1–S5 зелёные на ≥2 OEM → MVP-0 |
| **MVP-0** | «Поймай мысль» (free), F1–F7 | ⛔ заблокирован Wave 0 | activation >40%, W4-retention >25% → MVP-1 |
| **MVP-0.x** | Доп. интеграции (Notion, Google Cal, Todoist) | ⏸ позже | — |
| **MVP-1** | «Встречи/лекции» + биллинг, M1–M5 | ⏸ стаб | Free→Paid 3–6%, маржа >50% → V1 |
| **V1** | Ассистент / выбор ИИ / iOS, V1.1–V1.5 | ⏸ стаб | — |
| **V2+** | B2B/Enterprise, SDK | ⏸ стаб | — |

## Wave 0 — спайки (детально)

| Spike | EARS-суть | Блокирующий? | Статус |
|---|---|---|---|
| S1 | mic-FGS переживает screen-off ≥60 мин | да | 🔜 |
| S2 | CDM-автозапуск по BT из фона | да | 🔜 |
| S3 | Активация tile / media-button <1 сек | да | 🔜 |
| S4 | RuStore Pay SDK sandbox: подписка+рекуррент | да | 🔜 |
| S5 | STT-стриминг латентность <2 сек на 4G | да | 🔜 |
| S6 | Русский on-device wake-word (бюджет батареи ≤3%/ч) | нет (оценочный) | 🔜 |

## Харнесс

| Компонент | Статус |
|---|---|
| `.planning/` спина (PROJECT/STATUS/JOURNAL/OQ) | ✅ |
| `.claude/agents/` (11 ролей) | ✅ |
| ADR-001..009 | ✅ |
| Спеки Wave 0 (S1–S6) | ✅ |
| Спеки MVP-0 (F1–F7) | ✅ |
| CI (Android/backend/security) | ✅ шаблоны (не подключены к secrets) |
| Код `app/` + `backend/` | ⛔ ещё нет (стартует в MVP-0 после Wave 0) |

## Следующее действие

Фаундер: разовый ops-setup (RuStore dev-аккаунт ИП, keystore, ключи Yandex Cloud/GigaChat, billing-sandbox, 2–3 физ-OEM) → диспетч **WAVE0-S1** автономной сессией (`planner`).
