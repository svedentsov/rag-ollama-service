import React from 'react';
import styles from './StatusIndicator.module.css';

/**
 * Пропсы для компонента StatusIndicator.
 */
interface StatusIndicatorProps {
    /** Текст, описывающий текущий этап выполнения задачи (например, "Ищу информацию..."). */
    statusText: string;
    /** Количество секунд, прошедших с начала операции. */
    elapsedTime: number;
}

/**
 * Отображает информативный статус выполнения асинхронной задачи.
 * Включает анимированный спиннер, текстовое описание этапа и секундомер.
 * @param {StatusIndicatorProps} props - Пропсы компонента.
 */
export const StatusIndicator: React.FC<StatusIndicatorProps> = ({ statusText, elapsedTime }) => {
    return (
        <div className={styles.statusWrapper} role="status" aria-live="polite">
            <div className={styles.spinner} aria-hidden="true"></div>
            <div className={styles.statusText}>
                <span>{statusText}</span>
                <span className={styles.timer} aria-label={`Прошло ${elapsedTime} секунд`}>{elapsedTime}s</span>
            </div>
        </div>
    );
};
