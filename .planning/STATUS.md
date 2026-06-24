<!-- AUTO-MAINTAINED by memory-curator at every phase-close. Manual edits may be overwritten. -->
# STATUS — роллинг-статус AIEAR

**Обновлено:** 2026-06-24 · **Текущая волна:** Wave 0 (спайки риска, блокирующая) · **Активная фаза:** WAVE0-S1 (PR-гейт открыт; решение фаундера)

## Сводка волн

| Wave | Цель | Статус | Гейт |
|---|---|---|---|
| **Wave 0** | Спайки риска S1–S6 (5 «гейтов взлёта» + 1 оценочный) | 🟡 в работе (S1 на PR-гейте) | S1–S5 зелёные на ≥2 OEM → MVP-0 |
| **MVP-0** | «Поймай мысль» (free), F1–F7 | ⛔ заблокирован Wave 0 | activation >40%, W4-retention >25% → MVP-1 |
| **MVP-0.x** | Доп. интеграции (Notion, Google Cal, Todoist) | ⏸ позже | — |
| **MVP-1** | «Встречи/лекции» + биллинг, M1–M5 | ⏸ стаб | Free→Paid 3–6%, маржа >50% → V1 |
| **V1** | Ассистент / выбор ИИ / iOS, V1.1–V1.5 | ⏸ стаб | — |
| **V2+** | B2B/Enterprise, SDK | ⏸ стаб | — |

## Wave 0 — спайки (детально)

| Spike | EARS-суть | Блокирующий? | Статус |
|---|---|---|---|
| S1 | mic-FGS переживает screen-off ≥60 мин | да | 🟡 PR-гейт · аудит `pass-with-followups` · код/тесты зелёные по доказательству (unit 7/7 незав.); **device-прогон + green-build — на фаундере** (2 evidence_gap) |
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
| ADR-001..010 | ✅ |
| Регламент evidence-backed автономности (ADR-010, handbook 07) | ✅ |
| Спеки Wave 0 (S1–S6) | ✅ |
| Спеки MVP-0 (F1–F7) | ✅ |
| CI (Android/backend/security) | ✅ шаблоны (не подключены к secrets) |
| Код `app/` + `backend/` | 🟡 `app/` скелет + mic-FGS спайк (S1); `backend/` ещё нет |

## Следующее действие

**Фаундер на гейте WAVE0-S1** (PR открыт, не смержен — tier 4):
1. **Ратифицировать** 2 evidence_gap (`needs-device`, `needs-google-maven-egress`) + пины версий / `minSdk=34` / имя ветки (см. PR + `AUDIT-WAVE0-S1.md`).
2. **Прогнать** `specs/wave-0/evidence/WAVE0-S1/run-on-device.sh` на ≥2 физ-OEM (**Xiaomi обяз.**), 60 мин screen-off + Doze → залить `device-logs/` (закрывает `needs-device` = реальный S1-GO).
3. **Green-build**: пере-прогнать полный self-run на раннере с Google-Maven-egress (закрывает `needs-google-maven-egress`).
4. Решение гейта: `merge / revise / abort`. S1-GO ещё **не** взят (device-доказательство pending).

Затем: следующий спайк **WAVE0-S2** (CDM-автозапуск) автономной сессией.
