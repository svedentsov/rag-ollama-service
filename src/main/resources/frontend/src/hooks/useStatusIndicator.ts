import { useState, useEffect } from 'react';

/**
 * @description Хук для управления состоянием индикатора загрузки. Инкапсулирует
 * логику таймера, чтобы избежать ненужных перерисовок родительских компонентов.
 *
 * @param {boolean} isRunning - Флаг, указывающий, активна ли операция, для которой отображается статус.
 * @param {string | null} statusText - Текст текущего статуса (например, "Ищу информацию...").
 * @param {number | null} startTime - Временная метка начала выполнения задачи в миллисекундах.
 *
 * @returns {string} Отформатированную строку статуса для отображения в UI (например, "Ищу информацию... 5s") или пустую строку.
 */
export function useStatusIndicator(isRunning: boolean, statusText: string | null, startTime: number | null): string {
    const [elapsedTime, setElapsedTime] = useState(0);

    useEffect(() => {
        let timer: number | undefined;

        if (isRunning && startTime) {
            // Устанавливаем таймер, который будет обновлять только внутреннее состояние этого хука.
            timer = window.setInterval(() => {
                setElapsedTime(Math.round((Date.now() - startTime) / 1000));
            }, 1000);
        } else {
            // Сбрасываем таймер и время, когда операция завершается.
            clearInterval(timer);
            setElapsedTime(0);
        }

        // Очистка при размонтировании компонента или изменении зависимостей.
        return () => window.clearInterval(timer);
    }, [isRunning, startTime]);

    if (!isRunning || !statusText) {
        return '';
    }

    return `${statusText} ${elapsedTime}s`;
}
