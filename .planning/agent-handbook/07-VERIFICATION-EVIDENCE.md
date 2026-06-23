# 07 — VERIFICATION & EVIDENCE (self-run тестов + live-gold перед PR)

> Канон [ADR-010](../decisions/ADR-010-evidence-backed-autonomy.md): **автономность внутри фазы подтверждается результатами, а не утверждениями.** Фаза не доходит до PR без evidence-бандла. Owner правила — `verifier`; соблюдают все.

## 1. Принцип

«Зелёное по утверждению» запрещено — только «зелёное по доказательству». Каждый закрытый EARS-критерий обязан иметь **воспроизводимое evidence** (вывод прогона, ссылку на FTL-ран, файл с метриками). Нет evidence → критерий не закрыт.

## 2. Что есть «gold» и «live-gold»

| Термин | Что | Когда обязателен |
|---|---|---|
| **gold** | прогон по golden dataset (детерминированные эталонные вход→выход) | AI-фазы (F2, M2, F7) |
| **live-gold** | gold/приёмочные сценарии против **реальных** сервисов/устройств (live SpeechKit/GigaChat/YandexGPT, RuStore sandbox, реальные OEM через FTL + agent-device) — **не моки**; даёт реальные WER / pass-rate / латентность / выживаемость | везде, где технически возможно |
| **mock** | прогон против заглушек | только для быстрой обратной связи в разработке; **не закрывает** фазу сам по себе |

## 3. Правило self-run (до хендофа в ревью)

Имплементер сам прогоняет и прикладывает вывод **прежде** чем отдать в ревью:

- **android-engineer:** `ktlintCheck detekt testDebugUnitTest` (+ Compose screenshot, где есть). Для нативного — собрать инструментальный набор для verifier.
- **backend-engineer:** `ruff check . && mypy --strict . && pytest` (unit + integration, вкл. unhappy-path).

«Написал тесты» ≠ «тесты зелёные». Хендоф `code.commit` без зелёного self-run + поля `evidence` → `reviewer` возвращает (`review.revision`).

## 4. Правило live-gold (verifier / evaluator, до PR)

- **verifier** реально гоняет EARS-как-тесты + **live-gold там, где возможно**: device-матрица (Gradle Managed Devices → FTL) + agent-device для нативного; live-сервисы для бэк-контрактов. Прикладывает evidence на каждый критерий.
- **evaluator** (AI-фазы) гоняет golden + adversarial против **live** STT/LLM, прикладывает WER + pass-rate. Mock-only прогон AI-фазу не закрывает.

## 5. Где live невозможно — явный gap (не тихий скип)

Нет ключей / устройства / sandbox / внешка лежит → **запрещено** молча зачесть mock-зелёным. Вместо этого:

1. Эмить `status: blocked`, `blocker_type: needs-live-evidence` **ИЛИ** пометить `evidence_gap` в хендофе.
2. Указать **причину** и **что нужно** (какие creds / какое устройство / какой sandbox).
3. Поднять фаундеру. Фаза идёт на гейт только если фаундер **явно принял** gap (тогда вердикт `pass-with-followups` с followup-таском на добор evidence).

## 6. Evidence-бандл

Куда: `specs/<wave>/evidence/<PHASE>/`. Что внутри (по применимости):

| Файл/ссылка | Содержит |
|---|---|
| `selftest-<role>.txt` | вывод lint/typecheck/unit/integration self-run |
| `verify-acceptance.md` | таблица EARS-критерий ↔ тест ↔ результат (pass/fail) + ссылки |
| `ftl-runs.md` | ссылки на Firebase Test Lab прогоны по OEM-матрице |
| `live-gold-<artifact>.json` | WER / classification accuracy / pass-rate / латентность с реальных сервисов |
| `device-logs/` | logcat / agent-device доказательства нативного поведения |
| `coverage.xml` / summary | покрытие (≥70% новый / ≥85% security-critical) |

Ссылается из хендоф-поля `evidence`, из `AUDIT-<PHASE>.md` (линза live-gold/evidence) и из тела PR (секция Evidence).

## 7. Гейты, на которых это проверяется

```
self-run (impl) ──► reviewer проверяет наличие evidence
                          │
                          ▼
                    verifier гоняет EARS + live-gold, пополняет evidence-бандл
                          │
                          ▼
                    architect: phase-аудит, линза live-gold/evidence (8-я)
                          │  нет воспроизводимого evidence → fail (или принятый gap)
                          ▼
                    memory-curator: PR с секцией Evidence → founder-гейт
```

## 8. Связь с автономными сессиями

Автономная сессия фазы (фаундер авторизовал) **обязана** дойти до полного evidence-бандла перед открытием PR. Именно бандл делает автономность доверенной: фаундер на гейте видит не «агент говорит, что работает», а **результаты прогонов**.
