import React from 'react';
import styles from './StatusIndicator.module.css';

/**
 * Пропсы для компонента StatusIndicator.
 */
interface StatusIndicatorProps {
    /** @param status - Полностью отформатированная строка статуса для отображения. */
    status: string;
}

/**
 * Отображает информативный статус выполнения асинхронной задачи.
 * Включает анимированный спиннер и отформатированный текст статуса.
 * @param {StatusIndicatorProps} props - Пропсы компонента.
 */
export const StatusIndicator: React.FC<StatusIndicatorProps> = ({ status }) => {
    if (!status) {
        return null;
    }

    // Извлекаем время из конца строки для стилизации
    const parts = status.match(/(.*)\s(\d+s)$/);
    const text = parts ? parts[1] : status;
    const time = parts ? parts[2] : '';

    return (
        <div className={styles.statusWrapper} role="status" aria-live="polite">
            <div className={styles.spinner} aria-hidden="true"></div>
            <div className={styles.statusText}>
                <span>{text}</span>
                {time && <span className={styles.timer} aria-label={`Прошло ${time}`}>{time}</span>}
            </div>
        </div>
    );
};
