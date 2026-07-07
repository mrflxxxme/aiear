# specs/_contracts — канонические контракты AIEAR

> **Единственный источник истины** для API, схем данных, промптов и форматов экспорта,
> разделяемых между Android-клиентом (`app/`) и backend'ом (`backend/`). Канон закреплён
> ADR-013 (§3) и производным решением A4 grill-протокола 2026-07-07.

## Правила

1. **Спеки ссылаются, не дублируют.** Спека фазы (`specs/<wave>/*.md`) указывает на файл
   контракта и описывает только фазо-специфичное поведение. Дублирование схемы в прозе
   спеки = блокер ревью (рассинхрон спека↔контракт, ADR-013).
2. **Правка любого файла здесь = tripwire «публичные контракты»** (ADR-011 D2,
   `.claude/autonomy/tripwire.yaml`): дифф, задевающий `specs/_contracts/`, требует
   founder-ack перед merge. Runner паузит на PR автоматически.
3. **Версионирование.** Контракты v1 соответствуют пути `/v1/*`. Ломающее изменение =
   новая версия (`/v2/*`, `thought.schema` с новым `$id`), не тихая правка v1.
4. **Клиент и сервер генерируют/валидируют по этим файлам**, а не по копиям в коде.
   Промпт LLM в `backend/app/ai/` — материализация `prompts/structure_v1.md`, CI сверяет.

## Состав

| Файл | Что фиксирует | Кто потребляет |
|---|---|---|
| [`openapi.yaml`](openapi.yaml) | OpenAPI 3.1 контракт `/v1/*`: auth (device-token, ADR-012), STT-стрим (WebSocket), `/v1/structure`, `/v1/sync` (delta+LWW) | F1, F2, F5, S4, S5 |
| [`thought.schema.json`](thought.schema.json) | Каноническая JSON Schema `Thought` (inbox-first статусная модель, ADR-015) | F1, F2, F3, F5, F6 |
| [`prompts/structure_v1.md`](prompts/structure_v1.md) | Канонический промпт LLM-структурирования v1 (ru) | F2, evaluator |
| [`golden/README.md`](golden/README.md) | Структура golden dataset'ов F2/F7 + пороги гейтов | F2, F7, evaluator |
| [`obsidian-note-format.md`](obsidian-note-format.md) | Формат .md-экспорта в Obsidian (SAF + URI-fallback) | F3 |

## Связанные ADR

- **ADR-012** — модель auth: аноним device-token → VK ID / Яндекс ID (MVP-1).
- **ADR-013** — backend в Yandex Cloud; контракты живут здесь; изменение = tripwire.
- **ADR-015** — inbox-first: `Thought` персистится локально до любых экспортов.
