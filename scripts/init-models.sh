#!/bin/sh

# Список моделей для загрузки
MODELS="llama3 phi3 mxbai-embed-large"

echo "Ожидание готовности Ollama..."
# Простая проверка, что Ollama отвечает, перед началом загрузки
until curl -s http://ollama:11434/api/tags > /dev/null; do
  >&2 echo "Ollama недоступен - ожидание..."
  sleep 2
done

echo "Ollama готов. Начало параллельной загрузки моделей..."

# Запускаем загрузку каждой модели в фоновом режиме
for model in $MODELS
do
    echo "Запуск загрузки модели: $model"
    (
      curl -s http://ollama:11434/api/pull -d "{\"name\": \"$model\"}" > /dev/null && echo "✅ Модель '$model' успешно загружена." || echo "❌ Ошибка при загрузке модели '$model'."
    ) &
done

# Ожидаем завершения всех фоновых процессов
wait
echo "Инициализация моделей завершена."