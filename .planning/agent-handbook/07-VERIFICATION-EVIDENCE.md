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

Для **нативных** фаз действует гибрид (grill 2026-07-07, решение 4.1): фаза без device-evidence МОЖЕТ закрыться `pass-with-followups`, runner продолжает следующую фазу, но **merge PR в `main` блокирован** до device-evidence ИЛИ явного founder-ack на gap (RUN-QUEUE-запись). См. ADR-010 §Решение п.5 / ADR-011 D3-native.

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

## 6a. Машинный манифест (ADR-011 D3 · единый путь evidence, grill 2026-07-07 / A5)

Машинная часть evidence живёт **внутри того же бандла**, а не в отдельном корневом `evidence/`:

- Каждая фаза с local-only гейтами из ADR-011 D3 (live-gold STT/LLM · RuStore sandbox · `device_survival` · adversarial audit · judge-панель) обязана иметь **`specs/<wave>/evidence/<PHASE>/manifest.json`**, перечисляющий **ВСЕ** гейты DoD фазы в `required_gates`, + по файлу `specs/<wave>/evidence/<PHASE>/<gate>.json` на гейт (схема — [`.claude/autonomy/evidence-schema.json`](../../.claude/autonomy/evidence-schema.json), `head_sha` = финальный коммит, `verdict: PASS`).
- Проверяет `scripts/autonomy/verify_evidence.py` (workflow `ci-evidence`): discovery по `specs/*/evidence/*/manifest.json`, свежесть + PASS каждого гейта.
- **Отсутствие/пустота манифеста = fail для нативных/AI-фаз** (runner гоняет верификатор с `--require`), а не «OK». «Нет манифеста → OK» допустим только для фаз, у которых local-only гейтов нет.

## 6b. Ярусы device-петли

```
CI-раннер (lint/unit/assemble, есть Google-egress)
   → Docker-эмулятор фаундера (установлен, работает) — smoke-ярус: быстрый прогон
     instrumented/выживаемости до трат FTL-минут; НЕ заменяет OEM-прогон
   → GMD/FTL (облачная OEM-матрица: Pixel, Samsung)
   → физ-OEM фаундера (Xiaomi обязателен — OEM-киллеры)
```

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

## 9. Известные ограничения облачной песочницы (уроки WAVE0-S1)

Автономная сессия часто работает в эфемерной облачной песочнице с **политикой egress**. Проверено
на WAVE0-S1: **Google Maven (`maven.google.com`/`dl.google.com`) может быть закрыт 403** →
AGP/AndroidX/Compose/Android SDK недоступны → **Android Gradle-сборка не идёт вообще** (падает на
резолве зависимостей, не на коде); часто также **нет `/dev/kvm`** (нет эмулятора) и **нет
device/FTL-кредов**. Правило `/root/.ccr/README.md`: 403/407-политику **не обходить** — репортить.

Карта, где что верифицируется:

| Проверка | В облачной сессии | Где живёт live |
|---|---|---|
| Pure-Kotlin/JVM-логика (seam) | ✅ `kotlinc` + JUnit с Maven Central | сессия |
| Android lint/unit/assemble | ⚠️ только при Google-Maven egress; иначе `evidence_gap: needs-google-maven-egress` | **GitHub CI-раннер** (egress есть) |
| Инструментальные (screen-off, Doze, выживаемость) | ❌ нет device/KVM → `evidence_gap: needs-device` | **GMD/FTL** + **физ-OEM фаундера** |

Паттерн: выноси из класса чистый, JVM-тестируемый **seam** (детектор/парсер) — его прогонишь в любой
песочнице как поведенческое прокси-evidence; «тяжёлую» проверку помечай явным `evidence_gap` с
указанием, **где** она закрывается (CI-раннер / устройство), и приложи runnable-артефакт (тест +
founder-run-kit), чтобы gap закрывался одним прогоном, а не переписыванием. И помни: тест надёжности,
который может дать **ложный green** (см. чек-лист в `memory/android-oem.md`), — не evidence.
