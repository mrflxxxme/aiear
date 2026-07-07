# ONBOARDING-SECRETS — founder-чеклист полной автономии

> Единый свод **ручного контура фаундера**: всё, что агенты не могут сделать сами (секреты, инфра, данные, устройства, одноразовые флипы). Сведено из `.claude/autonomy/README.md` §Founder one-time actions, `.claude/autonomy/BUILD-PLAN.md` §Активация, `.env.example`, CI-workflows. Основание — grill 2026-07-07 (решения 4.2, 4.3, 3.4; протокол: [`_session-context/GRILL-2026-07-07-project-docs.md`](_session-context/GRILL-2026-07-07-project-docs.md)). Фаундер подтвердил готовность завести все 4 секрета в **1–2 недели**.
>
> Формат: зачем → какую фазу разблокирует → куда класть → статус. Закрыл пункт — меняй ⬜ на ✅ (с датой).

## (a) Секреты / аккаунты

| # | Что | Зачем | Разблокирует | Куда класть | Статус |
|---|---|---|---|---|---|
| a1 | **Yandex SpeechKit API-ключ** (+ folder id) | live-gold STT: реальная латентность/WER, не мок (ADR-010) | **WAVE0-S5** (стриминг <2с), далее F1/F2/F6 | `.env`: `YANDEX_SPEECHKIT_API_KEY`, `YANDEX_CLOUD_FOLDER_ID`; CI-секреты `YANDEX_SPEECHKIT_API_KEY`, `YANDEX_CLOUD_FOLDER_ID` (ci-backend live) | ⬜ |
| a2 | **GigaChat ключ** (client id + secret) | live-gold LLM: классификация ≥85% на golden (реш. 3.3) | **MVP0-F2** (структурирование), далее F7 | `.env`: `GIGACHAT_CLIENT_ID`, `GIGACHAT_CLIENT_SECRET`; одноимённые CI-секреты | ⬜ |
| a3 | **RuStore dev-аккаунт ИП + Pay sandbox** | подписка + рекуррент в sandbox — фундамент биллинга MVP-1 | **WAVE0-S4** | `.env`: `RUSTORE_PAY_MERCHANT_ID`, `RUSTORE_PAY_KEY`; одноимённые CI-секреты | ⬜ **стартовать СРАЗУ** — верификация ИП в RuStore долгая, дольше остальных |
| a4 | **GCP service-account + Firebase Test Lab** | облачная device-петля (OEM-матрица Pixel/Samsung) — device-evidence без физ-устройства | device-гейты **всех нативных фаз** (S1–S3, S6, F1, F4…) | CI-секрет `GCP_SA_KEY` (json service-account; используется `ci-android`) | ⬜ |

Ключи — **только** в CI-secrets / `.env` (в `.gitignore`); никогда в код/контекст агента (CLAUDE.md §3, tripwire `secrets_keys_crypto`).

## (b) Инфра

| # | Что | Зачем | Разблокирует | Куда/как | Статус |
|---|---|---|---|---|---|
| b1 | **Egress-allowlist**: `maven.google.com` + `dl.google.com` | без Google Maven Android-сборка в облачной сессии не идёт вообще (урок WAVE0-S1, handbook 07 §9) | Android-сборки в автономных сессиях (все S/F-нативные) | панель управления egress-политикой org-прокси (Q9 закрыт реш. 4.2) | ⬜ |
| b2 | **CI-secrets в GitHub** | гейты реально гоняют то, что заявляют (условие 2 активации auto-merge) | `ci-android` (FTL), `ci-backend` (live), `ci-security` | GitHub → Settings → Secrets and variables → Actions (имена — колонка «куда класть» в §a) | ⬜ |
| b3 | **Canonical funded `.env`** в main-worktree | preflight runner'а: `provision_env.py` раздаёт live-gold-доступ фазам (exit 2 = live недоступен → stuck AI-фаз) | live-gold всех AI/backend-фаз | скопируй `.env.example` → `.env` в корне main-worktree, заполни §a | ⬜ |
| b4 | **Branch protection на `main`** | require PR + required checks (`ci-android`/`ci-backend`/`ci-security`/`ci-evidence`) + linear history + enforce_admins + delete-branch-on-merge | активация auto-merge (D1) | GitHub → Settings → Branches | ⬜ (при активации) |
| b5 | **Вооружить pre-merge хук** | defense-in-depth растяжки D2 — Claude не может self-install hook-конфиг | активация auto-merge | мёржни `.claude/autonomy/settings.hook-snippet.json` в `.claude/settings.json` | ⬜ (при активации) |
| b6 | **`notify.json`** (опц.) | Telegram phone-ack на 5 interrupt-событиях (D8) | удобство ack; без него — desktop push + RUN-QUEUE | `.claude/autonomy/notify.json`: `{"telegram_chat_id": "<id>"}` | ⬜ (опц.) |

## (c) Данные

| # | Что | Зачем | Разблокирует | Куда класть | Статус |
|---|---|---|---|---|---|
| c1 | **50–100 реальных ru-реплик** (текстом достаточно) | seed golden dataset: агент размечает и аугментирует до 300+ (перефразы/шум/adversarial), фаундер выборочно ратифицирует разметку (реш. 3.4) | **MVP0-F2** (классификация ≥85%), далее F7 | `specs/_contracts/golden/` | ⬜ до F2 |

## (d) Device-петля

| # | Что | Зачем | Разблокирует | Как | Статус |
|---|---|---|---|---|---|
| d1 | **Физ-Xiaomi батч-прогоны** | OEM-киллеры (MIUI) не покрываются FTL — Xiaomi обязателен в device-evidence (≥2 OEM) | **гейт Wave 0** (S1–S5 green ≥2 OEM); первый — run-kit `specs/wave-0/evidence/WAVE0-S1/run-on-device.sh` | батчем перед гейтом волны (не по одной фазе) | ⬜ |
| d2 | **Docker-эмулятор** | smoke-ярус device-петли между CI и FTL/физ-устройствами (handbook 07 §6b) | быстрая обратная связь нативных фаз без трат FTL-минут | — | ✅ уже установлен и работает у фаундера (реш. 4.2) |

## (e) Одноразово — флип auto-merge

| # | Что | Условия | Как | Статус |
|---|---|---|---|---|
| e1 | **Флип auto-merge** (снять rails-first паузу-на-каждом-PR) | все **3** (BUILD-PLAN §Активация): ① Wave-0 S1–S5 зелёные на ≥2 OEM · ② CI на secrets (b2) · ③ evidence-контур реально работает (единый путь + машинный DoD + `verify_evidence.py --require`) | сделай b4 + b5 (+b6), затем убери паузу из `run.md` §Merge + обнови README-баннер + BUILD-PLAN | ⬜ |
