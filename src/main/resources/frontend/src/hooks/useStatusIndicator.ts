import { useState, useEffect } from 'react';
import { useStreamingStore } from '../state/streamingStore';

/**
 * Форматирует прошедшее время в секундах.
 * @param {number} seconds - Количество секунд.
 * @returns {string} Отформатированная строка (например, "5s").
 */
const formatTime = (seconds: number): string => `${seconds}s`;

/**
 * Хук для управления состоянием индикатора загрузки.
 * Инкапсулирует логику таймера и форматирования статусного текста.
 * @param {boolean} isRunning - Флаг, указывающий, активна ли операция.
 * @param {string | null} statusText - Текст текущего статуса (например, "Ищу информацию...").
 * @returns {string} Отформатированную строку статуса для отображения в UI или пустую строку.
 */
export function useStatusIndicator(isRunning: boolean, statusText: string | null, startTime: number | null): string {
    const [currentTime, setCurrentTime] = useState(Date.now());

    useEffect(() => {
        let timer: number | undefined;
        if (isRunning) {
            // Обновляем текущее время каждую секунду для перерисовки таймера
            timer = window.setInterval(() => {
                setCurrentTime(Date.now());
            }, 1000);
        } else {
            clearInterval(timer);
        }
        return () => window.clearInterval(timer);
    }, [isRunning]);

    if (!isRunning || !startTime) {
        return '';
    }

    const elapsedTime = Math.round((currentTime - startTime) / 1000);
    const baseText = statusText || 'Думаю...';

    return `${baseText} ${formatTime(elapsedTime)}`;
}
