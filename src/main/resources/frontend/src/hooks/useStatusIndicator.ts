import { useState, useEffect } from 'react';

/**
 * Форматирует прошедшее время в секундах.
 * @param {number} seconds - Количество секунд.
 * @returns {string} Отформатированная строка (например, "5s").
 */
const formatTime = (seconds: number): string => `${seconds}s`;

/**
 * Хук для управления состоянием индикатора загрузки, инкапсулирующий логику таймера.
 * @param {boolean} isRunning - Флаг, указывающий, активна ли операция.
 * @param {string | null} statusText - Текст текущего статуса (например, "Ищу информацию...").
 * @param {number | null} startTime - Временная метка начала выполнения задачи в миллисекундах.
 * @returns {string} Отформатированную строку статуса для отображения в UI или пустую строку.
 */
export function useStatusIndicator(isRunning: boolean, statusText: string | null, startTime: number | null): string {
    const [elapsedTime, setElapsedTime] = useState(0);

    useEffect(() => {
        let timer: number | undefined;

        if (isRunning && startTime) {
            // Устанавливаем таймер, который будет обновлять только внутреннее состояние хука
            timer = window.setInterval(() => {
                setElapsedTime(Math.round((Date.now() - startTime) / 1000));
            }, 1000);
        } else {
            // Сбрасываем таймер и время, когда операция завершается
            clearInterval(timer);
            setElapsedTime(0);
        }

        // Очистка при размонтировании компонента или изменении зависимостей
        return () => window.clearInterval(timer);
    }, [isRunning, startTime]); // Эффект зависит только от isRunning и startTime

    if (!isRunning) {
        return '';
    }

    const baseText = statusText || 'Думаю...';

    return `${baseText} ${formatTime(elapsedTime)}`;
}
