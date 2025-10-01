import { useState, useEffect } from 'react';

/**
 * Кастомный хук для "откладывания" обновления значения (debouncing).
 * @param value - Значение, которое нужно отложить.
 * @param delay - Задержка в миллисекундах.
 * @returns Отложенное значение, которое обновится только после того,
 * как `value` не менялось в течение `delay`.
 */
export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    // Устанавливаем таймер, который обновит значение после задержки
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    // Очищаем таймер при каждом изменении `value` или `delay`.
    // Это и есть суть debouncing.
    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
}
