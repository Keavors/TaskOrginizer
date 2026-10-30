# Органайзер задач

Настольный органайзер для Windows: вкладки со списками задач и заметками, которые можно надиктовать голосом. Распознавание речи работает без интернета и пишет английские названия внутри русской речи латиницей: «залить проект на GitHub», а не «на гитхаб».

> Работа в процессе. План — в [docs/PLAN.md](docs/PLAN.md), устройство — в [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), выбор модели речи — в [docs/ASR-BENCHMARK.md](docs/ASR-BENCHMARK.md).

## Разработка

Нужен Node.js 24 LTS.

```bash
npm install      # заодно скачивает движок whisper.cpp в resources/whisper
npm run dev      # запуск в режиме разработки
npm test         # юнит-тесты
npm run dist     # установщик в dist/
```
