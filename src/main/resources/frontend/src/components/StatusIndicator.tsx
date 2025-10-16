import React, { FC, useState, useEffect } from 'react';
import styles from './StatusIndicator.module.css';

/**
 * @interface StatusIndicatorProps
 * @description Пропсы для компонента StatusIndicator.
 */
interface StatusIndicatorProps {
    /** @param {string} status - Текст статуса для отображения. */
    status: string;
    /** @param {number | null} startTime - Временная метка начала операции в мс. */
    startTime: number | null;
}

/**
 * Форматирует прошедшее время в секундах.
 * @param {number} seconds - Количество секунд.
 * @returns {string} Отформатированная строка (например, "5s").
 */
const formatTime = (seconds: number): string => `${seconds}s`;

/**
 * Отображает информативный статус выполнения асинхронной задачи с таймером.
 * Компонент инкапсулирует логику таймера, чтобы предотвратить ненужные
 * перерисовки родительских компонентов.
 *
 * @param {StatusIndicatorProps} props - Пропсы компонента.
 * @returns {React.ReactElement | null} Отрендеренный компонент или null, если статус пуст.
 */
export const StatusIndicator: FC<StatusIndicatorProps> = ({ status, startTime }) => {
    const [elapsedTime, setElapsedTime] = useState(0);

    useEffect(() => {
        let timer: number | undefined;
        if (startTime) {
            // Инициализируем таймер сразу
            setElapsedTime(Math.round((Date.now() - startTime) / 1000));
            // И запускаем интервал для его обновления
            timer = window.setInterval(() => {
                setElapsedTime(Math.round((Date.now() - startTime) / 1000));
            }, 1000);
        } else {
            // Сбрасываем, если startTime отсутствует
            setElapsedTime(0);
        }
        // Очистка при размонтировании
        return () => window.clearInterval(timer);
    }, [startTime]); // Эффект зависит только от startTime

    if (!status) {
        return null;
    }

    return (
        <div className={styles.statusContent} role="status" aria-live="polite">
            <div className={styles.spinner} aria-hidden="true"></div>
            <div className={styles.statusText}>
                <span>{status}</span>
                {startTime && <span className={styles.timer} aria-label={`Прошло ${elapsedTime} секунд`}>{formatTime(elapsedTime)}</span>}
            </div>
        </div>
    );
};
