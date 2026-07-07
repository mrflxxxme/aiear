# Wave 0 — Спайки риска (5 «гейтов взлёта» + 1 оценочный) 🔴 БЛОКИРУЮЩАЯ

> Цель: доказать выполнимость до продуктовой разработки. Агент пробует (`native-spike-debugger` / `backend-engineer`), фаундер валидирует на реальном устройстве. Детальные spike-спеки — [`specs/wave-0/`](../../specs/wave-0/).

## Спайки

| Spike | Что доказываем | Блокирующий | Спека |
|---|---|---|---|
| **S1** | mic-FGS переживает screen-off ≥60 мин | да | [S1](../../specs/wave-0/S1-mic-fgs-screenoff.md) |
| **S2** | Автозапуск по BT через CompanionDeviceManager | да | [S2](../../specs/wave-0/S2-cdm-bt-autostart.md) |
| **S3** | Активация без рук (tile + media-button) <1 сек | да | [S3](../../specs/wave-0/S3-handsfree-activation.md) |
| **S4** | RuStore Pay SDK sandbox: подписка + рекуррент | да | [S4](../../specs/wave-0/S4-rustore-pay-sandbox.md) |
| **S5** | STT-стриминг латентность <2 сек на 4G (**WebSocket** default — ADR-013/A1; Yandex Cloud `ru-central1`) | да | [S5](../../specs/wave-0/S5-stt-streaming-latency.md) |
| **S6** | Русский on-device wake-word **«Эй, бадди»** на взведённом сервисе | нет (оценочный) | [S6](../../specs/wave-0/S6-ru-wakeword.md) |

## Гейт

**S1–S5 зелёные на ≥2 OEM** (вкл. Xiaomi/Samsung) → переход к MVP-0. **S6** — оценочный: исход определяет скоуп MVP-0.x (едет ли wake-word), но не блокирует MVP-0.

## Риск-фокус

Главный риск проекта (R-OEM) — OEM-убийцы фона (MIUI/Honor/Transsion). Облачная верификация (Firebase Test Lab) покрывает ~80%; OEM-killers требуют **физической** проверки фаундером на 2–3 реальных телефонах. Don't-Kill-My-App матрица — обязательна.

**Device-петля (Q9 закрыт решением, grill 2026-07-07 §4.2):** FTL (Pixel/Samsung) + **Docker-эмулятор фаундера** (уже установлен и работает — промежуточный smoke-ярус) + физ-Xiaomi батчем; egress-allowlist `maven.google.com`/`dl.google.com`. Исполнение — ONBOARDING-SECRETS.md.

## Зависимость

Ничего из MVP-0 не стартует, пока гейт Wave 0 не зелёный. Это не «фаза разработки», а доказательство, что разработка вообще поедет.
