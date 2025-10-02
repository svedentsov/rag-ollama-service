import { useState, useEffect } from 'react';

/**
 * Форматирует прошедшее время в секундах.
 * @param seconds - Количество секунд.
 * @returns Отформатированная строка (например, "5s").
 */
const formatTime = (seconds: number): string => `${seconds}s`;

/**
 * Хук для управления состоянием индикатора загрузки.
 * Инкапсулирует логику таймера и форматирования статусного текста.
 * @param isRunning - Флаг, указывающий, активна ли операция.
 * @param statusText - Текст текущего статуса (например, "Ищу информацию...").
 * @returns Отформатированную строку статуса для отображения в UI.
 */
export function useStatusIndicator(isRunning: boolean, statusText: string | null): string {
    const [elapsedTime, setElapsedTime] = useState(0);

    useEffect(() => {
        let timer: number | undefined;
        if (isRunning) {
            setElapsedTime(0); // Сбрасываем таймер при каждом новом запуске
            timer = window.setInterval(() => {
                setElapsedTime(prev => prev + 1);
            }, 1000);
        } else {
            clearInterval(timer);
            setElapsedTime(0);
        }
        return () => window.clearInterval(timer);
    }, [isRunning]);

    if (!isRunning) {
        return '';
    }

    const baseText = statusText || 'Думаю...';
    return `${baseText} ${formatTime(elapsedTime)}`;
}
