import React, { FC } from 'react';
import styles from './StatusIndicator.module.css';

/**
 * @interface StatusIndicatorProps
 * @description Пропсы для компонента StatusIndicator.
 */
interface StatusIndicatorProps {
    /** @param {string} status - Полностью отформатированная строка статуса для отображения. */
    status: string;
}

/**
 * Отображает информативный статус выполнения асинхронной задачи.
 * В этой версии компонент стал "глупым" и отвечает только за рендеринг
 * спиннера и текста, без внешнего контейнера.
 * @param {StatusIndicatorProps} props - Пропсы компонента.
 * @returns {React.ReactElement | null} Отрендеренный компонент или null, если статус пуст.
 */
export const StatusIndicator: FC<StatusIndicatorProps> = ({ status }) => {
    if (!status) {
        return null;
    }

    // Извлекаем время из конца строки для стилизации (например, "Ищу информацию... 5s")
    const parts = status.match(/(.*)\s(\d+s)$/);
    const text = parts ? parts[1] : status;
    const time = parts ? parts[2] : '';

    return (
        <div className={styles.statusContent} role="status" aria-live="polite">
            <div className={styles.spinner} aria-hidden="true"></div>
            <div className={styles.statusText}>
                <span>{text}</span>
                {time && <span className={styles.timer} aria-label={`Прошло ${time}`}>{time}</span>}
            </div>
        </div>
    );
};
