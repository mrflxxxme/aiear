# MVP-0 — «Поймай мысль» (free, доводим до блеска) 🟢 ЯДРО

> Цикл: **поймал голосом → ИИ распознал/структурировал → предложил действие → зафиксировал в destination.** Монетизации нет (free, без карты). Детальные EARS — [`specs/mvp-0/`](../../specs/mvp-0/).

## Фазы

| Фаза | Способность | Спека | Pipeline |
|---|---|---|---|
| **F1** | Захват мысли (tile/BT/кнопка/media-hook/wake-word → стриминг STT) | [F1](../../specs/mvp-0/F1-thought-capture.md) | fullstack |
| **F2** | ИИ-структурирование + классификация (LLM) | [F2](../../specs/mvp-0/F2-ai-structuring.md) | fullstack (+evaluator) |
| **F3** | Фиксация в действие (Obsidian/Календарь/Telegram/Share) | [F3](../../specs/mvp-0/F3-action-integrations.md) | fullstack |
| **F4** | Онбординг наушников (автоопределение, тест активации, tile-гайд) | [F4](../../specs/mvp-0/F4-headphone-onboarding.md) | android (+designer) |
| **F5** | Локальная база + история + поиск (sync в RF-облако) | [F5](../../specs/mvp-0/F5-local-history-sync.md) | fullstack |
| **F6** | Офлайн-капчер (on-device STT при отсутствии сети) | [F6](../../specs/mvp-0/F6-offline-capture.md) | android |
| **F7** | Голосовые команды в сессии (разметка/destination/подтверждение) | [F7](../../specs/mvp-0/F7-voice-commands.md) | fullstack (+evaluator) |

## Порядок и зависимости

F1 (захват) → F2 (структурирование) → F3 (фиксация) — критический путь ядра. F4 (онбординг) параллелен. F5 (история/sync) после F3. F6 (офлайн) и F7 (голос-команды) — после стабильного F1–F3. F7 зависит от исхода S6 для wake-word-части (но команды в сессии — независимо).

## Гейт MVP-0 → MVP-1

activation >40% (D1 завершил первый capture→destination) · W1/W4 retention (W4 >25%) · captures/user/нед · share-rate. Меряем реальное, без vanity (ADR-008).

## <a id="mvp-0x-fast-follow"></a>MVP-0.x — fast-follow (доп. интеграции)

Notion API · Google Calendar (OAuth) · Todoist · Яндекс.Календарь. Плюс — wake-word в продукт, **если спайк S6 зелёный** (ADR-009). Спеки раскрывает `planner` JIT после гейта MVP-0.
